package library;

public class Outer {

    public static class Mid {

        public static class Deeper {

            public Deeper() {
                throw new java.lang.Error();
            }
        }
    }

    public static class Nested {

        public Nested() {
            throw new java.lang.Error();
        }
    }
}
