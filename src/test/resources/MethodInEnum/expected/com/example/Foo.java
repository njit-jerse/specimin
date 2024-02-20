package com.example;

class Foo {

    private enum STATUS {

        ON {

            public void testing() {
                throw new Error();
            }
        }
        , OFF {

            public void testing() {
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
