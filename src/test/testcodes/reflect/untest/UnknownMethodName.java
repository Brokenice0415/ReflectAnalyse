package untest;

import java.lang.reflect.Method;

public class UnknownMethodName {

    public static void main(String[] args) throws Exception {
        invokeNonArg(unknown("nonArg"));
        invokeOneArg(unknown("oneArg"), new Class[]{D.class});
        invokeOneArg(unknown("oneArg"), new Class[]{Object.class});
        invokeOneArg(unknown("oneArg"), new Class[]{E.class});
        invokeOneArgRet(unknown("oneArgRet"), new Class[]{D.class});
    }

    static void invokeNonArg(String name) throws Exception {
        Method nonArg = E.class.getMethod(name);
        nonArg.invoke(null); // <untest.E: void nonArg()>
    }

    static void invokeOneArg(String name, Class<?>[] paramTypes) throws Exception {
        Method oneArg = E.class.getMethod(name, paramTypes);
        E e = new E();
        oneArg.invoke(e, e); // <untest.E: void oneArg(untest.D)>, <untest.E: void oneArg(Object)>, <untest.E: untest.D oneArgRet(untest.D)>
                             // <untest.D: void oneArg(untest.E)>
    }

    static D invokeOneArgRet(String name, Class<?>[] paramTypes) throws Exception {
        Method oneArg = E.class.getMethod(name, paramTypes);
        E e = new E();
        D d = (D) oneArg.invoke(e, e); // <untest.E: untest.D oneArgRet(untest.D)>
        return d;
    }

    static String unknown(String s) {
        return new String(s);
    }
}

class D {

    public void oneArg(D d) {
        System.out.println("untest.D.oneArg(untest.D)");
    }

    public void oneArg(E e) {
        System.out.println("untest.D.oneArg(untest.E)");
    }
}

class E extends D {

    static void packagePrivate() {
        System.out.println("untest.E.packagePrivate()");
    }

    public static void nonArg() {
        System.out.println("untest.E.nonArg()");
    }

    public void oneArg(Object o) {
        System.out.println("untest.E.oneArg(Object)");
    }

    @Override
    public void oneArg(D d) {
        System.out.println("untest.E.oneArg(untest.D)");
    }

    public void oneArg(String s) {
        System.out.println("untest.E.oneArg(String)");
    }

    public D oneArgRet(D d) {
        System.out.println("untest.E.oneArgRet(untest.D)");
        return d;
    }
}
