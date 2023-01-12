package untest;

import java.lang.reflect.Method;

public class UnknownClassName {

    public static void main(String[] args) throws Exception {
        invokeOneArg(unknown("untest.F"), new Class[]{Object.class}, new F());
        invokeOneArg(unknown("untest.H"), new Class[]{Object.class}, new H());
        invokeOneArg(unknown("untest.H"), new Class[]{String.class}, new H());
    }

    static void invokeOneArg(String name, Class<?>[] paramTypes, Object recv) throws Exception {
        Class<?> c = Class.forName(name);
        Method oneArg = c.getMethod("oneArg", paramTypes);
        String arg = "arg";
        oneArg.invoke(recv, arg); // <untest.F: void oneArg(Object)>,
                                  // <untest.G: void oneArg(Object)>,
                                  // <untest.H: void oneArg(String)>
    }

    static String unknown(String s) {
        return new String(s);
    }
}

class F {

    public void nonArg() {
        System.out.println("untest.F.nonArg()");
    }

    public void oneArg(Object o) {
        System.out.println("untest.F.oneArg(Object)");
    }

    public void oneArg2(Object o) {
        System.out.println("untest.F.oneArg2(Object)");
    }
}

class G {

    public void oneArg(Object o) {
        System.out.println("untest.G.oneArg(Object)");
    }
}

class H extends G {

    public void oneArg(String s) {
        System.out.println("untest.H.oneArg(String)");
    }

    public void twoArgs(Object o1, Object o2) {
        System.out.println("untest.H.twoArgs(Object,Object)");
    }
}
