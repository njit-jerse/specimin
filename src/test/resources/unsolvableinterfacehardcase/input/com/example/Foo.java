package com.example;

import java.util.List;

// Suppose this is an isolated Java program from a real life project, and all of the implementation
// for methods from List are in Baz. Then, Specimin removes List<E> from the class declaration,
// even though add() is called (and creates a synthetic add() method instead). This isn't the right
// behavior, but Specimin is currently limited without access to the full program.
class Foo<E> extends Baz<E> implements List<E> {
    public void bar(E input) {
        this.add(input);
    }

}
