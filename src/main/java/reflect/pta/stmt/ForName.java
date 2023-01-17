package reflect.pta.stmt;

import reflect.pta.elem.Variable;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.Stmt;
import soot.tagkit.LineNumberTag;

import java.util.Objects;

/**
 * Created by liture on 2021/9/20 11:08 下午
 *
 * 内存分配语句;
 * 如: x = new T()
 */
public class ForName extends Statement {

    private SootClass sootClass;

    public ForName(String className) {
        this.sootClass = null;
        for (SootClass clazz : Scene.v().getApplicationClasses()) {
            if (clazz.toString().equals(className)) {
                this.sootClass = clazz;
                break;
            }
        }
    }

    public SootClass getSootClass() {
        return this.sootClass;
    }

    public SootMethod getSootMethod(String methodName) {
        for (SootMethod method : sootClass.getMethods()) {
            if (methodName.equals("\"" + method.getName() + "\"")) {

                //args


                return method;
            }
        }
        return null;
    }


    public String getType() {
        return sootClass.moduleName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ForName that = (ForName) o;
        return Objects.equals(sootClass, that.sootClass);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sootClass);
    }

    @Override
    public String toString() {
        String stmtStr = sootClass.toString() + ".forName";
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

