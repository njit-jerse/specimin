package com.example;

import com.other.a.Foo;

public class Target {

    String target(Foo foo) {
        Rule rule = new Impl();
        return rule.apply(foo);
    }
}
