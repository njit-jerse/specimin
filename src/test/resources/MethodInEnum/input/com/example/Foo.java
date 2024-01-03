package com.example;

class Foo {

    private enum STATUS{
        ON {
            public static void testing() {
                throw new RuntimeException();
            }
        },
        OFF {
            public static void testing() {
                throw new RuntimeException();
            }
        };
        public abstract void testing();
    }

    private void bar() {
        STATUS.ON.testing();
    }
}
