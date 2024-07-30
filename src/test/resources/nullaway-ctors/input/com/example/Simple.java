package com.example;

class Simple {
    private Object foo;

    private Foo bar;

    private int x;

    public Simple() {
        // No initialization of a field can change NullAway's output,
        // so the NullAway modularity model needs to preserve all fields
        // whenenver the target is a constructor.
    }
}
