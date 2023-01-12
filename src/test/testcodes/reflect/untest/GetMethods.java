package untest;

import java.lang.reflect.Method;

public class GetMethods {

    public static void main(String[] args) throws Exception {
        invokeOneArg("foo");
        invokeOneArg("bar");
    }

    static void invokeOneArg(String name) throws Exception {
        Method[] methods = J.class.getMethods();
        for (Method m : methods) {
            if (m.getName().equals(name)) {
                J j = new J();
                m.invoke(j, j); // <untest.I: void bar(untest.I)>, <untest.J: void foo(untest.J)>
            }
        }
    }
}

class I {

    public void bar(I i) {
        System.out.println("untest.I.bar(untest.I)");
    }

    void bar(J j) {
        System.out.println("untest.I.bar(untest.J)");
    }
}

class J extends I {

    public void foo(J j) {
        System.out.println("untest.J.foo(untest.J)");
    }

    void foo(String s) {
        System.out.println("untest.J.foo(String)");
    }
}
