package com.example;

import org.example.Op;

class Foo {

    public enum Mode {

        PREFIX(Op.EQ), CONTAINS(Op.CONTAINS), SPARSE(Op.NOT_EQ);

        Op op;

        Mode(Op op) {
            this.op = op;
        }
    }

    public void bar() {
        Mode mode = Mode.PREFIX;
    }
}
