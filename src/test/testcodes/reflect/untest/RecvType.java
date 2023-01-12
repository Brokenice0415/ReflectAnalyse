package untest;

import java.lang.reflect.Method;

public class RecvType {

    public static void main(String[] args) throws Exception {
        invoke(unknown("untest.K"), "foo");
        invoke(unknown("untest.K"), "bar");
    }

    static void invoke(String cName, String mName) throws Exception {
        Class<?> c = Class.forName(cName);
        Method m = c.getMethod(mName);
        K k = new K();
        m.invoke(k); // <untest.K: void foo()>, <untest.K: void bar()>
    }

    static String unknown(String s) {
        return new String(s);
    }
}

class K {

    public void foo() {
        System.out.println("untest.K.foo()");
    }

    public void bar() {
        System.out.println("untest.K.bar()");
    }

    public void baz() {
        System.out.println("untest.K.baz()");
    }
}
