package reflect.pta.stmt;

import reflect.pta.elem.Variable;
import soot.SootClass;
import soot.SootMethod;
import soot.Value;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;

import java.util.Objects;

public class GetMethod extends Statement{

    private SootClass baseClass;

    private Value base;

    private SootMethod sootMethod;

    private Value invokeBase;

    private Value invoke;

    public GetMethod(SootMethod method, SootClass baseClass, Value base, Value invoke, Value invokeBase) {
        this.base = base;
        this.sootMethod = method;
        this.baseClass = baseClass;
        this.invoke = invoke;
        this.invokeBase = invokeBase;
    }

    public SootClass getBaseClass() {
        return baseClass;
    }

    public Value getBase() {
        return base;
    }

    public SootMethod getSootMethod() {
        return sootMethod;
    }

    public Value getInvoke() {
        return invoke;
    }

    public Value getInvokeBase() {
        return invokeBase;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GetMethod that = (GetMethod) o;
        return Objects.equals(base, that.base) && Objects.equals(sootMethod, that.sootMethod)
                && Objects.equals(invoke, that.invoke) && Objects.equals(invokeBase, that.invokeBase);
    }

    @Override
    public int hashCode() {
        return Objects.hash(base, sootMethod, invoke, invokeBase);
    }

    @Override
    public String toString() {
        String stmtStr = base.toString() + ".getMethod " + invoke.toString();
        String superStr = super.toString();
        if (superStr.length() > 0) {
            return superStr + ": " + stmtStr;
        }
        StringBuilder buff = new StringBuilder();
        buff.append(stmtStr);
//        buff.append(stmt.toString());
//        buff.append("@");
//        buff.append(stmt.getTag(LineNumberTag.IDENTIFIER));
        return buff.toString();
    }
}
