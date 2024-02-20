package com.example;

class Foo {

    private enum STATUS{
        ON {
            int x, y;
        },
        OFF {
        };
    }

    private void bar() {
        STATUS.ON.x;
    }
}
