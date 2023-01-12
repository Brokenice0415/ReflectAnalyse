package reflect.pta.stmt;

import reflect.pta.elem.Field;
import reflect.pta.elem.Variable;

import java.util.Objects;

/**
 * Created by liture on 2021/9/20 11:08 下午
 *
 * 实例字段写操作;
 * 如: x.f = y
 */
public class StaticStore extends Statement {

    private Variable from;

    private Field field;

    public StaticStore(Field field, Variable from) {
        this.from = from;
        this.field = field;
    }

    /**
     * @return 赋值左侧的字段
     */
    public Field getField() {
        return field;
    }

    /**
     * @return 赋值右侧的变量
     */
    public Variable getFrom() {
        return from;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StaticStore store = (StaticStore) o;
        return Objects.equals(from, store.from) && Objects.equals(field, store.field);
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, field);
    }

    @Override
    public String toString() {
        return super.toString() + ": o." + field + " = " + from;
    }
}
