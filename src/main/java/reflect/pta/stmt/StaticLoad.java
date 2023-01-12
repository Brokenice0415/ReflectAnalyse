package reflect.pta.stmt;

import reflect.pta.elem.Field;
import reflect.pta.elem.Variable;

import java.util.Objects;

/**
 * Created by liture on 2021/9/20 11:09 下午
 *
 * 字段读操作;
 * 如: y = x.f
 */
public class StaticLoad extends Statement {

    private final Field field;

    private final Variable to;

    public StaticLoad(Variable to, Field field) {
        this.field = field;
        this.to = to;
    }

    /**
     * @return 读字段后写入的变量
     */
    public Variable getTo() {
        return to;
    }

    /**
     * @return 被读字段
     */
    public Field getField() {
        return field;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StaticLoad that = (StaticLoad) o;
        return Objects.equals(field, that.field) && Objects.equals(to, that.to);
    }

    @Override
    public int hashCode() {
        return Objects.hash(field, to);
    }

    @Override
    public String toString() {
        return super.toString() + ": " + to + " = o." + field;
    }
}
