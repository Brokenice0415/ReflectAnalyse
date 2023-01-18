package reflect.pta.elem;

import reflect.pta.analysis.ci.PointsToSet;
import reflect.pta.stmt.*;
import soot.*;
import soot.jimple.*;
import soot.jimple.internal.*;
import soot.tagkit.LineNumberTag;
import soot.tagkit.Tag;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Created by liture on 2021/9/20 11:07 下午
 *
 * 方法
 */
public class Method {

    private final SootMethod delegate;

    private Set<Statement> pointerAffectingStmt;

    private Map<Value, Variable> localMap;

    private Map<Variable, ForName> forNameMap;

    private Map<Variable, GetMethod> methodMap;

    public Method(SootMethod sootMethod) {
        this.delegate = sootMethod;
        initialize();
    }

    /**
     * Translate SootMethod to ours 'pointer-affecting' IR
     *
     * 'pointer-affecting' statements:
     * 1. New      x = new T()
     * 2. Assign   x = y
     * 3. Store    x.f = y
     * 4. Load     y = x.f
     * 5. Call     r = x.k(arg, ...)
     *
     * Java语言中的指针有：
     * 1. Local Variable: x          <- 考虑
     * 2. Static field: C.f           <- 在本作业中我们不考虑(一般实现的话处理成全局变量)
     * 3. instance field: x.f         <- 考虑
     * 4. Array element: array[i]    <- 在本作业中我们不考虑(一般实现的话处理成array[i]抽象成array.index_field)
     */
    private void initialize() {
        pointerAffectingStmt = new LinkedHashSet<>();

        // 避免重复生成对象
        localMap = new HashMap<>();

        forNameMap = new HashMap<>();
        methodMap = new HashMap<>();

        if (delegate == null) return;

        for (Unit unit : delegate.retrieveActiveBody().getUnits()) {
            Stmt stmt = (Stmt) unit;
            if (stmt instanceof AssignStmt) {
                AssignStmt assignStmt = (AssignStmt) stmt;
                Value l = assignStmt.getLeftOp();
                Value r = assignStmt.getRightOp();
                // x = new T
                if (l instanceof Local && r instanceof NewExpr) {
                    Variable x = getVariable((Local) l);
                    Allocation alloc = new Allocation(x, assignStmt);
                    addPointerAffectingStmt(stmt, alloc);
                }
                // x = y
                if (l instanceof Local && r instanceof Local) {
                    Variable x = getVariable((Local) l);
                    Variable y = getVariable((Local) r);
                    Assign assign = new Assign(y, x);
                    addPointerAffectingStmt(stmt, assign);
                }
                // y = x.f
                if (l instanceof Local && r instanceof InstanceFieldRef) {
                    Variable y = getVariable((Local) l);
                    Variable x = getVariable((Local) ((InstanceFieldRef) r).getBase());
                    Field f = new Field((InstanceFieldRef) r);
                    InstanceLoad load = new InstanceLoad(y, x, f);
                    x.getLoads().add(load);
                    addPointerAffectingStmt(stmt, load);
                }
//                if (l instanceof Local && r instanceof StaticFieldRef) {
//                    Variable y = getVariable((Local) l);
//                    Field f = new Field((StaticFieldRef) r);
//                    StaticLoad load = new StaticLoad(y, f);
//                    addPointerAffectingStmt(stmt, load);
//                }
                // x.f = y
                if (l instanceof InstanceFieldRef && r instanceof Local) {
                    Variable x = getVariable((Local) ((InstanceFieldRef) l).getBase());
                    Field f = new Field((InstanceFieldRef) l);
                    Variable y = getVariable((Local) r);
                    InstanceStore store = new InstanceStore(x, f, y);
                    x.getStores().add(store);
                    addPointerAffectingStmt(stmt, store);
                }
//                if (l instanceof StaticFieldRef && r instanceof Local) {
//                    Field f = new Field((StaticFieldRef) l);
//                    Variable y = getVariable((Local) r);
//                    StaticStore store = new StaticStore(f, y);
//                    x.getInstanceStores().add(store);
//                    addPointerAffectingStmt(stmt, store);
//                }
            }
            if (stmt.containsInvokeExpr()) {
                CallSite callSite = null;
                Variable x = null;

                InvokeExpr invokeExpr = stmt.getInvokeExpr();
                if (invokeExpr instanceof InstanceInvokeExpr) {

                    x = getVariable((Local) ((InstanceInvokeExpr) invokeExpr).getBase());

                    if (Pattern.matches(".*<java\\.lang\\.Class: java\\.lang\\.reflect\\.Method getMethod\\(java\\.lang\\.String,java\\.lang\\.Class\\[\\]\\)>.*", invokeExpr.toString())) {
                        if (stmt instanceof AssignStmt && ((AssignStmt) stmt).getLeftOp() instanceof Local) {
                            // r = x.k(arg, ...)
                            Variable r = getVariable((Local) ((AssignStmt) stmt).getLeftOp());

//                            System.out.println(r.toString() + " = " + x.toString() + "." + invokeExpr.getArg(0));

                            if (forNameMap.containsKey(x)) {
                                ForName forName = forNameMap.get(x);
                                SootMethod reflectMethod = forName.getSootMethod(invokeExpr.getArg(0).toString());
                                if (reflectMethod != null) {
                                    if (reflectMethod.isStatic()) {
                                        methodMap.put(r, new GetMethod(
                                                reflectMethod,
                                                forName.getSootClass(),
                                                (Local) ((AssignStmt) stmt).getLeftOp(),
                                                new JStaticInvokeExpr(reflectMethod.makeRef(), new ArrayList<>()),
                                                ((InstanceInvokeExpr) invokeExpr).getBase()
                                        ));
                                    }
                                    else {
                                        methodMap.put(r, new GetMethod(
                                                reflectMethod,
                                                forName.getSootClass(),
                                                (Local) ((AssignStmt) stmt).getLeftOp(),
                                                new JSpecialInvokeExpr((Local)((InstanceInvokeExpr) invokeExpr).getBase(), reflectMethod.makeRef(), new ArrayList<>()),
                                                ((InstanceInvokeExpr) invokeExpr).getBase()
                                        ));
                                    }
                                }


//                                System.out.println(r + " = " + methodMap.get(r).getInvokeStmt());
                            }
                            callSite = new CallSite(stmt, x, r);
                        }
                        else {
                            callSite = new CallSite(stmt, x);
                        }
                    }
//                    else if (Pattern.matches(".*<java\\.lang\\.reflect\\.Method: java\\.lang\\.Object invoke\\(java\\.lang\\.Object,java\\.lang\\.Object\\[\\]\\)>.*", invokeExpr.toString())) {
//
//                        GetMethod getMethod = getReflectMethod(x);
//
//                        if (getMethod == null) {
//                            if (stmt instanceof AssignStmt && ((AssignStmt) stmt).getLeftOp() instanceof Local) {
//                                // r = x.k(arg, ...)
//                                Variable r = getVariable((Local) ((AssignStmt) stmt).getLeftOp());
//                                callSite = new CallSite(stmt, x, r);
//                            } else {
//                                // x.k(arg, ...)
//                                callSite = new CallSite(stmt, x);
//                            }
//                        }
//                        else {
//
//                            Variable x1 = getVariable(getMethod.getBase());
//
//                            if (stmt instanceof AssignStmt && ((AssignStmt) stmt).getLeftOp() instanceof Local) {
//                                // r = x.k(arg, ...)
//                                Variable r = getVariable((Local) ((AssignStmt) stmt).getLeftOp());
//                                // TODO: x = ?
//
//                                AssignStmt assignStmt = new JAssignStmt(
//                                        ((AssignStmt) stmt).getLeftOp(),
//                                        getMethod.getinvoke()
//                                );
//
//                                callSite = new CallSite(assignStmt, x1, r);
//
//                                addPointerAffectingStmt(assignStmt, new Call(callSite));
//
//                                callSite = new CallSite(stmt, x1, r);
//                            } else {
//                                // x.k(arg, ...)
//
//                                InvokeStmt invokeStmt = new JInvokeStmt(getMethod.getinvoke());
//
////                                System.out.println(invokeStmt);
//
//                                callSite = new CallSite(invokeStmt, x1);
//
//                                addPointerAffectingStmt(invokeStmt, new Call(callSite));
//
//                                callSite = new CallSite(stmt, x1);
//                            }
//
//                        }
//                    }
                    else {
                        if (stmt instanceof AssignStmt && ((AssignStmt) stmt).getLeftOp() instanceof Local) {
                            // r = x.k(arg, ...)
                            Variable r = getVariable((Local) ((AssignStmt) stmt).getLeftOp());
                            callSite = new CallSite(stmt, x, r);
                        } else {
                            // x.k(arg, ...)
                            callSite = new CallSite(stmt, x);
                        }
                    }
                } else if (invokeExpr instanceof StaticInvokeExpr) {
                    // ClassName.k(arg, ...)   : static call

                    String invokeName = invokeExpr.toString();
                    if (Pattern.matches(".*<java\\.lang\\.Class: java\\.lang\\.Class forName\\(java\\.lang\\.String\\)>.*", invokeName)) {
                        // r = ClassName.k(arg, ...)
                        if (stmt instanceof AssignStmt && ((AssignStmt) stmt).getLeftOp() instanceof Local) {
                            Variable r = getVariable((Local) ((AssignStmt) stmt).getLeftOp());
//                            assert invokeExpr.getArgCount() == 1;
                            if (invokeExpr.getArg(0) instanceof StringConstant) {
                                // 需要常量传播
                                StringConstant str = (StringConstant) invokeExpr.getArg(0);
                                String className = str.value;

                                forNameMap.put(r, new ForName(className));
                            }
                            callSite = new CallSite(stmt, null, r);
                        }
                        else {
                            callSite = new CallSite(stmt);
                        }
                    }
                    else {
                        if (stmt instanceof AssignStmt && ((AssignStmt) stmt).getLeftOp() instanceof Local) {
                            // r = ClassName.k(arg, ...)
                            Variable r = getVariable((Local) ((AssignStmt) stmt).getLeftOp());
                            callSite = new CallSite(stmt, null, r);
                        } else {
                            callSite = new CallSite(stmt);
                        }
                    }
                }

                Call call = new Call(callSite);
                if (x != null) {
                    x.getCalls().add(call);
                }
                addPointerAffectingStmt(stmt, call);
            }
        }
//        for (Value v: localMap.keySet()) {
//            StringBuilder buff = new StringBuilder();
//            buff.append(
//                    ">>" + v + ": \n"
//            );
//            for (Call call: getVariable(v).getCalls()) {
//                buff.append(call + "\n");
//            }
//            buff.append("======");
//            System.out.println(buff);
//        }
    }

    private void addPointerAffectingStmt(Stmt stmt, Statement ir) {
        ir.setEnclosingMethod(this);
        Tag lineTag = stmt.getTag(LineNumberTag.IDENTIFIER);
        if (lineTag != null) {
            int line = Integer.parseInt(lineTag.toString());
            ir.setLine(line);
        }
        pointerAffectingStmt.add(ir);
    }

    public Variable getVariable(Value value) {
        return localMap.computeIfAbsent(value, k -> new Variable(value, this));
    }

    public List<Variable> getRetVariable() {
        List<Variable> variableList = new LinkedList<>();
        if (delegate == null) return variableList;
        for (Unit unit : delegate.getActiveBody().getUnits()) {
            if (unit instanceof ReturnStmt) {
                ReturnStmt returnStmt = (ReturnStmt) unit;
                Value v = returnStmt.getOp();
                if (v instanceof Local) {
                    variableList.add(getVariable((Local) v));
                }
            }
        }
        return variableList;
    }

    public List<Variable> getParams() {
        List<Variable> variableList = new LinkedList<>();
        if (delegate == null) return variableList;
        List<Local> locals = delegate.getActiveBody().getParameterLocals();
        for (Local local : locals) {
            Variable variable = getVariable(local);
            variableList.add(variable);
        }
        return variableList;
    }

    public Variable getThisVariable() {
        if (delegate == null) return null;
        Local thisLocal = delegate.getActiveBody().getThisLocal();
        return getVariable(thisLocal);
    }

    /**
     * 获取方法内的所有指针相关操作(Pointer-affecting)的语句
     * @return
     */
    public Set<Statement> getStatements() {
        return pointerAffectingStmt;
    }

    public SootMethod getSootMethod() {
        return delegate;
    }

    public GetMethod getReflectMethod(Variable var) {
        return methodMap.get(var);
    }

    public SootClass getReflectClass(Variable var) {
        return forNameMap.get(var).getSootClass();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Method method = (Method) o;
        return Objects.equals(delegate, method.delegate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(delegate);
    }

    @Override
    public String toString() {
        StringBuilder buff = new StringBuilder();
        buff.append(delegate.getSignature()).append(" {\n");
        for (Statement stmt : pointerAffectingStmt) {
            buff.append("\t").append(stmt).append("\n");
        }
        buff.append("}");
        return buff.toString();
    }
}
