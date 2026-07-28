package com.example;

public sealed class Foo permits Bar, Baz, Unsolved {
    public Foo() { }

    public Foo(int a, String b, Object c, char d, boolean e, Unsolved f) { }

    void foo() {
        Foo foo = new Foo(100, "", null, 'a', true, null);

        Bar bar = new Bar(0);
    }
}

final class Bar extends Foo {
    public Bar() {
        super();
    }

    public Bar(int x) {
        // This should become super(0, null, ...) because Foo's parameterless constructor will be removed
        super();

        // Should be removed...
        System.out.println();
        int x = 0;
    }
}

final class Baz extends Foo { }