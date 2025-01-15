package com.example;

public class Baz {

    public static class InnerBaz {

        public void toBeRemovedToo() {
            throw new java.lang.Error();
        }

        public static class NestedInnerBaz {
            public void test() {
                System.out.println("Hello World");
            }

            public void toBeRemoved() {
                throw new java.lang.Error();
            }
        }
    }
}
