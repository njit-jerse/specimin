package com.example;

class Foo {

    private enum Status{
        ON(1), OFF(0);

        int bitRep;

        Status(int x) {
            bitRep = x;
        }
    }

    private void bar() {
        int y = Status.ON.bitRep;
    }
}
