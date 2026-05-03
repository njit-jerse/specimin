package com.example;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

public class Simple {
    public void foo() {
        bar(Foo::myMethod);
        baz(Foo::myMethod2);

        Function<Foo, String> func = Foo::myMethod3;
        Predicate<Foo> pred = Foo::myMethod4;
    }

    void bar(BiFunction<Foo, String, ?> func) {
        throw new java.lang.Error();
    }

    void baz(Function<String, ? extends Number> func) {
        throw new java.lang.Error();
    }
}