package com.example;

class Foo {

    private enum Status {

        ON(1), OFF(0)
    }

    private void bar() {
        int y = Status.ON.bitRep;
    }
}