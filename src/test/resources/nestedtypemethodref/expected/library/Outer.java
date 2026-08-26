package library;

public class Outer {

    public static class Nested {

        public static java.lang.String foo() {
            throw new java.lang.Error();
        }
    }
}
