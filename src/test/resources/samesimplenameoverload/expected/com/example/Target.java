package com.example;

import com.example.a.Foo;

public class Target {

    String target(Foo foo) {
        Rule rule = new Impl();
        return rule.apply(foo);
    }
}
