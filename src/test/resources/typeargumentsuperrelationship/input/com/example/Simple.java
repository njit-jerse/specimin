package com.example;

public class Simple {
    void bar() {
        Foo<? extends Bar> foo = new Foo<Baz>();
        Foo<? extends Bar2> foo2 = new Foo<Baz2>();

        BadFoo<? extends Bar3> badFoo = new BadFoo<Baz3>(); 

        // Bar2 should extend Bar
        foo = foo2;
        // Foo should extend BadFoo, but Bar3 should have no super/subtypes
        badFoo = foo2;
    }
}
