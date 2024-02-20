package com.example;

class Foo {

    private enum STATUS {

        ON {

            public static void testing() {
                throw new Error();
            }
        }
        , OFF {

            public static void testing() {
                throw new Error();
            }
        }
        ;

        public abstract void testing();
    }

    private void bar() {
        STATUS.ON.testing();
    }
}
