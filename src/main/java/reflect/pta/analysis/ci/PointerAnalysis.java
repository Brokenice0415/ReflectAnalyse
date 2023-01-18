package reflect.pta.analysis.ci;

import reflect.cha.CallEdge;
import reflect.cha.CallKind;
import reflect.cha.JimpleCallGraph;
import reflect.pta.analysis.HeapModel;
import reflect.pta.elem.*;
import reflect.pta.stmt.*;
import soot.*;
import soot.jimple.*;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JInvokeStmt;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.toolkits.scalar.Pair;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Created by liture on 2021/9/20 11:11 下午
 *
 * 本类实现了指针分析算法(对应Lecture 10的115页)
 *
 * assignment4说:
 * 它已经实现了处理方法调用的路基了，我们不需要管了？(这个尝试自己实现吧...)
 */
public class PointerAnalysis {

    /**
     * 堆模型
     */
    protected HeapModel heapModel;

    protected SootMethod sootMethod;
    protected Method entry;

    protected WorkList WL;

    protected PointerFlowGraph PFG;

    protected Set<Statement> S;

    protected Set<Method> RM;

    protected JimpleCallGraph CG;

    protected Map<Call, Set<CallSite>> InvokeCallFrom;

    public PointerAnalysis(SootMethod entry) {
        this.sootMethod = entry;
        this.heapModel = HeapModel.v();
    }

    /**
     * 开始指针分析算法
     */
    public void solve() {
        initialize();
        analysis();
    }

    /**
     * 实现指针分析算法(Lecture 10, Page 115)的前两行;
     * 初始化各种数据结构，entry methods
     */
    protected void initialize() {
        InvokeCallFrom = new HashMap<>();

        WL = new WorkList();
        PFG = new PointerFlowGraph();
        S = new LinkedHashSet<>();
        RM = new LinkedHashSet<>();
        CG = new JimpleCallGraph();
        entry = new Method(sootMethod);
        addReachable(entry);
    }

    /**
     * 实现指针分析中的 WorkList 处理主循环
     */
    private void analysis() {
        while (!WL.isEmpty()) {
            Pair<Pointer, PointsToSet> entry = WL.remove();

            // propagate
            Pointer n = entry.getO1();
            PointsToSet pts = entry.getO2();
            PointsToSet delta = propagate(n, pts);

            if (!(n instanceof Var)) {
                continue;
            }

            // now, n represents a variable x
            Var x = (Var) n;

            for (Obj o : delta) {
//                // x.f = y
//                processInstanceStore(x, o);
//
//                // y = x.f
//                processInstanceLoad(x, o);

                // ProcessCall(x, o_i)
                processCall(x, o);
            }

        }
    }

    protected void addReachable(Method m) {
        if (!RM.contains(m)) {
            RM.add(m);

            // S U= S_m
            Set<Statement> S_m = m.getStatements();
            S.addAll(S_m);

//            for (Statement st: S_m) {
//                System.out.println(st.toString());
//            }
//            System.out.println("====");

            // foreach i: x = new T() \in S_m
            processAllocations(m);

            // foreach x = y \in S_m
            processLocalAssign(m);

            // foreach static call
            processStaticCall(m);

            processInvoke(m);
        }
    }

    /**
     * 这个方法实现集合的差运算: delta = pts - pts(n)
     * 另外还实现了Lecture 9, Page 43的 propagate(p, pts) 函数
     *
     * 这里合并了两个步骤到一个方法里面，用于降低冗余的计算
     * @param n
     * @param pts
     * @return pts - pts(n)
     */
    protected PointsToSet propagate(Pointer n, PointsToSet pts) {
        PointsToSet ptsOfn = n.getPointsToSet();
        PointsToSet delta = PointsToSet.difference(pts, ptsOfn);
        if (!delta.isEmpty()) {
            ptsOfn.union(delta);
            // foreach n -> s \in PFG
            for (Pointer s : PFG.getSuccessorOf(n)) {
                WL.addPointerEntry(s, delta);
            }
        }
        return delta;
    }

    /**
     * Lecture 9, Page 43 的 AddEdge 函数
     * @param s PFG边的source
     * @param t PFG边的destination
     */
    protected void addPFGEdge(Pointer s, Pointer t) {
        boolean add = PFG.addEdge(s, t);
        if (add) {
            WL.addPointerEntry(t, s.getPointsToSet());
        }
    }

    /**
     * 处理新的可达方法内的allocations；
     * Lecture 10, Page 118 的 AddReachable函数的第一个foreach循环
     *
     * 提示：
     * 需要实现堆模型(heap abstraction)来获取抽象对象(abstract object).
     * 我们使用allocation-size abstraction，以至于每个allocation site生成一个abstract object
     *
     * assignment4说:
     * 我们能够使用当前类的heapModel字段来获取堆模型上的抽象对象:
     *
     * Allocation alloc = ...;
     * Object allocSite = alloc.getAllocationSite();
     * Obj obj = heapModel.getObj(allocSite, alloc.getType(), method);
     *
     * 这个heapModel尝试自己实现吧...
     * @param m
     */
    protected void processAllocations(Method m) {
        Set<Statement> S_m = m.getStatements();
        S_m.stream()
            .filter(s -> s instanceof Allocation)
            .forEach(s -> {
                // i: x = new T()
                Allocation i = (Allocation) s;
                // o_i
                Obj o = heapModel.getObj(i.getAllocationSite(), i.getType(), m);
                // <x, o_i>
                WL.addPointerEntry(PFG.getVar(i.getVar()), PointsToSet.singleton(o));
            });
    }

    /**
     * 处理局部变量的赋值；
     * 如: x = y
     *
     * Lecture 10, Page 118 的 AddReachable函数的第二个foreach循环
     * @param m
     */
    protected void processLocalAssign(Method m) {
        Set<Statement> S_m = m.getStatements();
        S_m.stream()
            .filter(s -> s instanceof Assign)
            .forEach(s -> {
                // x = y
                Assign assign = (Assign) s;
                // y -> x
                addPFGEdge(PFG.getVar(assign.getFrom()), PFG.getVar(assign.getTo()));
            });
    }

    /**
     * 处理静态函数调用
     * @param m
     */
    protected void processStaticCall(Method m) {
        Set<Statement> S_m = m.getStatements();
        S_m.stream()
                .filter(s -> (s instanceof Call)
                        && CallKind.STATIC == CallKind.getCallKind(((Call) s).getCallSite().getCallSite()))
                .forEach(s -> {
                    // l: r = ClassName.k(a1, ..., an)
                    Call call = (Call) s;
                    CallSite callSite = call.getCallSite();
                    StaticInvokeExpr staticCall = (StaticInvokeExpr) callSite.getCallSite().getInvokeExpr();

                    String invokeName = staticCall.toString();

                    if (Pattern.matches(".*<java\\.lang\\.Class: java\\.lang\\.Class forName\\(java\\.lang\\.String\\)>.*", invokeName)) {
                        assert staticCall.getArgCount() == 1;
                        if (staticCall.getArg(0) instanceof StringConstant) {
                            // TODO
                            // 需要常量传播
                            StringConstant str = (StringConstant) staticCall.getArg(0);
                            String classType = str.value;

                            Allocation allocation = new Allocation(call.getCallSite().getRet(), classType);
                            Obj o = heapModel.getObj(allocation.getAllocationSite(), classType, m);
                            WL.addPointerEntry(PFG.getVar(call.getCallSite().getRet()), PointsToSet.singleton(o));
                        }
                    }
                    else { // normal static method
                        // 被调用函数 (不需要Dispatch, 静态可知)
                        SootMethod calleeSootMethod = staticCall.getMethod();
                        Method calleeMethod = null;
                        for (Method mm : RM) {
                            if (mm.getSootMethod() == calleeSootMethod) {
                                calleeMethod = mm;
                                break;
                            }
                        }
                        if (calleeMethod == null) {
                            calleeMethod = new Method(calleeSootMethod);
                        }

                        processCallLink(m, calleeMethod, callSite);
                    }
                });
    }

    /**
     * 处理调用时的连边
     * @param curMethod 当前函数
     * @param calleeMethod 被调用函数
     * @param callSite 调用点
     */
    private void processCallLink(Method curMethod, Method calleeMethod, CallSite callSite) {
        if (!CG.contains(callSite.getCallSite(), calleeMethod.getSootMethod())) {
            // add c:l -> ct:m to CG
            CG.addEdge(callSite.getCallSite(),
                    calleeMethod.getSootMethod(),
                    CallKind.getCallKind(callSite.getCallSite()));

            addReachable(calleeMethod);

            // foreach parameter p_i of m do
            //   AddEdge(c:a_i, ct:p_i)
            InvokeExpr invoke = callSite.getCallSite().getInvokeExpr();
            for (int i = 0; i < calleeMethod.getParams().size(); i++) {
                Value argValue = invoke.getArg(i);
                if (argValue instanceof Local) {
                    Local arg = (Local) argValue;
                    Variable argVariable = curMethod.getVariable(arg);

                    Variable paramVariable = calleeMethod.getParams().get(i);

                    addPFGEdge(PFG.getVar(argVariable), PFG.getVar(paramVariable));
                }
                else if (argValue instanceof StringConstant) {
                    StringConstant arg = (StringConstant) argValue;
                    Variable argVariable = curMethod.getVariable(arg);

                    Variable paramVariable = calleeMethod.getParams().get(i);

                    addPFGEdge(PFG.getVar(argVariable), PFG.getVar(paramVariable));
                }
            }

            // AddEdge(ct:m_ret, c:r)
            Variable callerRetVar = callSite.getRet();
            if (callerRetVar != null) {
                List<Variable> calleeRetVariableList = calleeMethod.getRetVariable();
                for (Variable calleeRetVar : calleeRetVariableList) {
                    addPFGEdge(PFG.getVar(calleeRetVar), PFG.getVar(callerRetVar));
                }
            }
        }
    }

    /**
     * 处理字段的写语句;
     * 如: x.f = y
     *
     * Lecture 10, Page 124 Solve函数中处理store语句的foreach循环
     * @param var pts改变的指针节点, 对应的指针变量为x
     * @param o 改变的部分delta
     */
    protected void processInstanceStore(Var var, Obj o) {
        Set<InstanceStore> stores = var.getVariable().getStores();
        // x.f = y
        for (InstanceStore store : stores) {
            // y -> o_i.f
            addPFGEdge(PFG.getVar(store.getFrom()), PFG.getInstanceField(o, store.getField()));
        }
    }

    /**
     * 处理字段读语句;
     * 如: y = x.f
     *
     * Lecture 10, Page 124 Solve函数中处理load语句的foreach循环
     * @param var pts改变的指针节点, 对应的指针变量为x
     * @param o 改变的部分delta
     */
    protected void processInstanceLoad(Var var, Obj o) {
        Set<InstanceLoad> loads = var.getVariable().getLoads();
        // y = x.f
        for (InstanceLoad load : loads) {
            // o_i.f -> y
            addPFGEdge(PFG.getInstanceField(o, load.getField()), PFG.getVar(load.getTo()));
        }
    }

    protected void processInvoke(final Method m) {
        Set<Statement> S_m = m.getStatements();
        S_m.stream()
                .filter(s -> Pattern.matches(".*<java\\.lang\\.reflect\\.Method: java\\.lang\\.Object invoke\\(java\\.lang\\.Object,java\\.lang\\.Object\\[\\]\\)>.*", s.toString()))
                .forEach(s -> {
//                    System.out.println(s.toString());
                    assert s instanceof Call;
                    Call call = (Call) s;

                    Value instanceValue = call.getCallSite().getCallSite().getInvokeExpr().getArg(0);
                    Variable instance = m.getVariable(instanceValue);
                    Var instanceVar = PFG.getVar(instance);

                    List<Value> paramsValues = call.getCallSite().getCallSite().getInvokeExpr().getArgs();
                    paramsValues.remove(0);

                    Variable x = call.getCallSite().getReceiver();
                    GetMethod getMethod = m.getReflectMethod(x);

                    if (getMethod != null) {
                        CallSite callSite;
                        if (s instanceof AssignStmt && ((AssignStmt) s).getLeftOp() instanceof Local) {
                            // r = x.k(arg, ...)
                            Variable r = m.getVariable((Local) ((AssignStmt) s).getLeftOp());
                            // TODO: x = ?
                            AssignStmt assignStmt = new JAssignStmt(
                                    ((AssignStmt) s).getLeftOp(),
                                    getMethod.getInvoke()
                            );
                            callSite = new CallSite(assignStmt, instance, r);
                        } else {
                            // x.k(arg, ...)
                            InvokeStmt invokeStmt = new JInvokeStmt(getMethod.getInvoke());
                            callSite = new CallSite(invokeStmt, instance);
                        }

                        Call newCall = new Call(callSite);
                        newCall.setEnclosingMethod(m);
                        newCall.setLine(call.getLine());

                        instance.getCalls().add(newCall);


                        // invoke call target method

                        if (InvokeCallFrom.containsKey(newCall)) {
                            InvokeCallFrom.get(newCall).add(call.getCallSite());
                        }
                        else {
                            Set<CallSite> callSiteSet = new HashSet<>();
                            callSiteSet.add(call.getCallSite());
                            InvokeCallFrom.put(newCall, callSiteSet);
                        }


//                        SootMethod dm = dispatch(getMethod.getBaseClass(), getMethod.getSootMethod());
//
//                        if (!CG.contains(call.getCallSite().getCallSite(), dm)) {
//                            // add l -> m to CG
//                            CG.addEdge(call.getCallSite().getCallSite(), dm, CallKind.getCallKind(call.getCallSite().getCallSite()));
//                        }

                    }
            });
    }

    /**
     * 处理函数调用，型如：l: r = var.k(a1, ..., an)
     * @param var
     * @param o
     */
    protected void processCall(Var var, Obj o) {
        if (var.getVariable() == null) return;
        Method curMethod = var.getVariable().getMethod();

        Set<Call> calls = var.getVariable().getCalls();
        for (Call call : calls) {

//            System.out.println(var + "\t" + "\t" + call);

            // r = var.k(a1, ..., an)
            CallSite callSite = call.getCallSite();

            // m = Dispatch(o_i, k)
            Method m = dispatch(o, callSite);

            // add <m_this, {o_i}> to WL
            WL.addPointerEntry(PFG.getVar(m.getThisVariable()), PointsToSet.singleton(o));

            if (InvokeCallFrom.containsKey(call)) {
                for (CallSite invokeFrom: InvokeCallFrom.get(call)) {
                    if (!CG.contains(invokeFrom.getCallSite(), m.getSootMethod())) {
                        // add l -> m to CG
                        CG.addEdge(invokeFrom.getCallSite(), m.getSootMethod(), CallKind.getCallKind(invokeFrom.getCallSite()));

                        addReachable(m);

                        // foreach parameter p_i of m do
                        //   AddEdge(a_i, p_i)
                        InvokeExpr invoke = invokeFrom.getCallSite().getInvokeExpr();
                        for (int i = 0; i < m.getParams().size(); i++) {
                            // 第一个参数为实例类型
                            Local arg = (Local) invoke.getArg(i+1);
                            Variable argVariable = curMethod.getVariable(arg);

                            Variable paramVariable = m.getParams().get(i);

                            addPFGEdge(PFG.getVar(argVariable), PFG.getVar(paramVariable));
                        }

                        // AddEdge(m_ret, r)
                        Variable callerRetVar = invokeFrom.getRet();
                        if (callerRetVar != null) {
                            List<Variable> calleeRetVariableList = m.getRetVariable();
                            for (Variable calleeRetVar : calleeRetVariableList) {
                                addPFGEdge(PFG.getVar(calleeRetVar), PFG.getVar(callerRetVar));
                            }
                        }

                        processCallLink(curMethod, m, invokeFrom);
                    }
                }
            }
            else {
                // if l -> m not in CG
                if (!CG.contains(callSite.getCallSite(), m.getSootMethod())) {
                    // add l -> m to CG
                    CG.addEdge(callSite.getCallSite(), m.getSootMethod(), CallKind.getCallKind(callSite.getCallSite()));

                    addReachable(m);

                    // foreach parameter p_i of m do
                    //   AddEdge(a_i, p_i)
                    InvokeExpr invoke = callSite.getCallSite().getInvokeExpr();
                    for (int i = 0; i < m.getParams().size(); i++) {
                        Local arg = (Local) invoke.getArg(i);
                        Variable argVariable = curMethod.getVariable(arg);

                        Variable paramVariable = m.getParams().get(i);

                        addPFGEdge(PFG.getVar(argVariable), PFG.getVar(paramVariable));
                    }

                    // AddEdge(m_ret, r)
                    Variable callerRetVar = callSite.getRet();
                    if (callerRetVar != null) {
                        List<Variable> calleeRetVariableList = m.getRetVariable();
                        for (Variable calleeRetVar : calleeRetVariableList) {
                            addPFGEdge(PFG.getVar(calleeRetVar), PFG.getVar(callerRetVar));
                        }
                    }

                    processCallLink(curMethod, m, callSite);
                }
            }
        }
    }

    protected Method dispatch(Obj o, CallSite callSite) {
        SootClass sootClass;
        if (o.getAllocSite() instanceof AssignStmt) {
            NewExpr newExpr = (NewExpr) ((AssignStmt) o.getAllocSite()).getRightOp();
            RefType refType = (RefType) newExpr.getType();
            sootClass = refType.getSootClass();
        } else {
            sootClass = new SootClass((String) o.getAllocSite());
        }

        InvokeExpr invokeExpr = callSite.getCallSite().getInvokeExpr();
        SootMethod sootMethod = invokeExpr.getMethod();

        SootMethod dispatch = dispatch(sootClass, sootMethod);

        Method method = null;
        for (Method m : RM) {
            if (m.getSootMethod() == dispatch) {
                method = m;
                break;
            }
        }
        if (method == null) {
            method = new Method(dispatch);
        }
        return method;
    }

    /**
     * @see reflect.cha.CHACallGraphBuilder
     */
    private SootMethod dispatch(SootClass sootClass, SootMethod method) {
        for (SootMethod m : sootClass.getMethods()) {
            if (!m.isAbstract()) {
                // fixme 这里判断方法签名匹配有点粗糙
                // fixme 应该是找Type-Compatible的
                if (m.getName().equals(method.getName())
                        && m.getParameterCount() == method.getParameterCount()) {
                    // 没有参数列表，那么直接匹配到了
                    if (m.getParameterCount() == 0) {
                        return m;
                    }
                    // 否则对比参数列表
                    for (int i = 0; i < m.getParameterCount(); i++) {
                        Type t = m.getParameterType(i);
                        Type t1 = method.getParameterType(i);
                        if (t.toQuotedString().equals(t1.toQuotedString())) {
                            return m;
                        }
                    }
                }
            }
        }
        SootClass superClass = sootClass.getSuperclassUnsafe();
        if (superClass != null) {
            return dispatch(superClass, method);
        }
        return null;
    }

}
