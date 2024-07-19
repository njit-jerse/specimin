package com.example;

public class Simple {
    @Bar(10)
    @Baz(foo = "foo", bar = Simple.class)
    @Foo(x = @Deprecated, y = {@Anno(1), @Anno(2)})
    public static int baz(String bar) {
        return 1;
    }
}
