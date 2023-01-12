package reflect.pta.elem;

import reflect.pta.stmt.Call;
import reflect.pta.stmt.InstanceLoad;
import reflect.pta.stmt.InstanceStore;
import soot.Local;
import soot.Value;
import soot.jimple.StringConstant;

import java.util.*;

/**
 * Created by liture on 2021/9/20 11:06 下午
 *
 * 表示value variables
 */
public class Variable {

    private final Value value;

    private final Method method;

    private final Set<InstanceStore> stores;

    private final Set<InstanceLoad> loads;

    private final Set<Call> calls;

    public Variable(Value value, Method method) {
        this.value = value;
        this.method = method;
        this.stores = new LinkedHashSet<>();
        this.loads = new LinkedHashSet<>();
        this.calls = new LinkedHashSet<>();
    }

    /**
     * @return 返回函数内对当前变量的store语句
     */
    public Set<InstanceStore> getStores() {
        return stores;
    }

    /**
     * @return 返回函数内对当前变量的load语句
     */
    public Set<InstanceLoad> getLoads() {
        return loads;
    }

    public Set<Call> getCalls() {
        return calls;
    }

    public Method getMethod() {
        return method;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Variable variable = (Variable) o;
        return Objects.equals(value, variable.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return toUniqueString();
    }

    public String toUniqueString() {
        if (value instanceof Local) {
            return method.getSootMethod().getSignature() + ": " + ((Local)value).getName();
        }
        else if (value instanceof StringConstant) {
            return method.getSootMethod().getSignature() + ": " + ((StringConstant)value).value;
        }
        return method.getSootMethod().getSignature() + ": ";
    }
}
