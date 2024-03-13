package com.example;

import org.example.Op;

class Foo {

    public enum Mode {

        PREFIX(Op.EQ), CONTAINS(Op.CONTAINS), SPARSE(Op.NOT_EQ);

        Mode(Op op) {
            throw new Error();
        }
    }

    public void bar() {
        Mode mode = Mode.PREFIX;
    }
}
