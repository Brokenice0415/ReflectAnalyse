package reflect.dataflow;

import soot.Local;
import soot.Value;
import soot.jimple.*;

import java.util.*;

/**
 * Created by liture on 2021/9/19 1:32 下午
 */
public class FlowMap {

    Map<Value, CPValue> delegateMap;

    public FlowMap() {
        this(new HashMap<>());
    }

    public FlowMap(Map<Value, CPValue> map) {
        this.delegateMap = map;
    }

    /**
     * 获取Local所关联的值
     * @param value soot中的局部变量
     * @return 如果存在关联的值, 返回该值；否则返回 {@link CPValue#getUndef()}
     */
    public CPValue get(Value value) {
        return delegateMap.computeIfAbsent(value, l -> CPValue.getUndef());
    }

    /**
     * 关联Local给定的格值
     * @param v soot中的局部变量
     * @param value 常量传播中的格值
     * @return 旧的值
     */
    public CPValue put(Value v, CPValue value) {
        return delegateMap.put(v, value);
    }

    /**
     * @return map中所有局部变量
     */
    public Set<Value> keySet() {
        return delegateMap.keySet();
    }

    /**
     * 从给定map拷贝内容到当前map
     * @param flowMap 给定另一个map
     * @return 拷贝操作改变当前map的内容，则返回true
     */
    public boolean copyFrom(FlowMap flowMap) {
        delegateMap.putAll(flowMap.delegateMap);
        return flowMap.delegateMap.equals(delegateMap);
    }

    /**
     * 给定赋值右侧的soot Value; 计算它的格值
     * @param sootValue 例如Local，BinaryExpression(整数的算数/比较运算)，IntConstant等
     * @return 计算得到的格值
     */
    public CPValue computeValue(Value sootValue) {
        if (sootValue instanceof Local) {
            return get((Local) sootValue);
        } else if (sootValue instanceof StringConstant) {
            return CPValue.makeConstant(((StringConstant) sootValue).value);
        } else if (sootValue instanceof BinopExpr) {
            BinopExpr binopExpr = (BinopExpr) sootValue;

            // 计算左侧格值
            Value op1 = binopExpr.getOp1();
            CPValue op1Val = computeValue(op1);

            // 计算右侧格值
            Value op2 = binopExpr.getOp2();
            CPValue op2Val = computeValue(op2);

            // 如果两个都未定义，那么整个计算也是未定义的
            if (op1Val == CPValue.getUndef() && op2Val == CPValue.getUndef()) {
                return CPValue.getUndef();
            }

            // 如果其中有一个未定义，那么整个计算就不是常数
            if (op1Val == CPValue.getUndef() || op2Val == CPValue.getUndef()) {
                return CPValue.getNAC();
            }

            // 其中有一个不是常数，那么整个计算就不是常数
            if (op1Val == CPValue.getNAC() || op2Val == CPValue.getNAC()) {
                return CPValue.getNAC();
            }

            // - - - - - - - 两个都是常量
            // 字符串相加
            if (binopExpr instanceof AddExpr) {
                return CPValue.makeConstant(op1Val.val() + op2Val.val());
            }
        }

        // 只考虑 Local / StringConstant / BinopExpr
        // 对于其它指令(比如函数调用等)，保守得 NAC
        return CPValue.getNAC();
    }

    // - - - - - - - - - static methods

    /**
     * 常量传播的Meet function：meet两个FlowMap并返回meet的结果
     * 用于处理控制流交汇
     * @param map1
     * @param map2
     * @return meet的结果Map
     */
    public static FlowMap meet(FlowMap map1, FlowMap map2) {
        FlowMap resultMap = new FlowMap();

        Set<Value> localSet = new HashSet<>();
        localSet.addAll(map1.keySet());
        localSet.addAll(map2.keySet());

        for (Value local : localSet) {
            CPValue v1 = map1.get(local);
            CPValue v2 = map2.get(local);
            CPValue meetVal = CPValue.meetValue(v1, v2);
            resultMap.put(local, meetVal);
        }

        return resultMap;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FlowMap flowMap = (FlowMap) o;
        return Objects.equals(delegateMap, flowMap.delegateMap);
    }

    @Override
    public int hashCode() {
        return Objects.hash(delegateMap);
    }

    @Override
    public String toString() {
        return delegateMap.toString();
    }
}
