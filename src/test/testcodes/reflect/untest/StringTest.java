package untest;

public class StringTest {

    public static void main(String[] args) {
        String a = "123";
        String b = "456";
        String c = stringAdd(a, b);
        String d = a + b;
    }

    public static String stringAdd(String s1, String s2) {
        return s1+s2;
    }
}
