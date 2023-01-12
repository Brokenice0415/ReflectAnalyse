package untest;

import java.lang.reflect.Method;

public class DuplicateName {

    public static void main(String[] args) throws Exception {
        invokePrint(new Class[]{Object.class}, new Object[]{"hello"});
        invokePrint(new Class[]{Object.class, Object.class},
                new Object[]{"hello", "hello"});
        invokePrint(new Class[]{Object.class, Object.class, Object.class},
                new Object[]{"hello", "hello", "hello"});
    }

    static void invokePrint(Class<?>[] paramTypes, Object[] args) throws Exception {
        Method print = C.class.getMethod("print", paramTypes);
        C c = new C();
        print.invoke(c, args); // <untest.C: void print(Object)>,
                               // <untest.C: void print(Object,Object)>,
                               // <untest.Parent: void print(Object,Object,Object)>
    }
}

class Parent {

    public void print(Object o) {
        System.out.println("untest.Parent.print(Object)");
    }

    public void print(Object o1, Object o2, Object o3) {
        System.out.println("untest.Parent.print(Object,Object,Object)");
    }
}

class C extends Parent {

    @Override
    public void print(Object o) {
        System.out.println("untest.C.print(Object)");
    }

    public void print(Object o1, Object o2) {
        System.out.println("untest.C.print(Object,Object)");
    }

    void print(Object o1, Object o2, Object o3, Object o4) {
        System.out.println("untest.C.print(Object,Object,Object,Object)");
    }
}
