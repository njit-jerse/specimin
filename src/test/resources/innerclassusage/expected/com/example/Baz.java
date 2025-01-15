package com.example;

public class Baz {

    public static class InnerBaz {

        public static class NestedInnerBaz {

            public void test() {
                throw new java.lang.Error();
            }
        }
    }
}
