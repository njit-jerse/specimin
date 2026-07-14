package com.example;

public sealed class Foo permits Bar, Baz, Unsolved {
    public Foo(int a, String b, Object c, char d, boolean e, Unsolved f) {
        throw new java.lang.Error();
    }

    void foo() {
        Foo foo = new Foo(100, "", null, 'a', true, null);

        Bar bar = new Bar(0);
    }
}

final class Bar extends Foo {
    public Bar(int x) {
        super(0, null, null, '\u0000', false, null);
    }
}

final class Baz extends Foo {
    public Baz() {
        super(0, null, null, '\u0000', false, null);
    }
}