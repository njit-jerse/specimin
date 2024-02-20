package com.example;

import org.testing.UnsolvedType;

class Foo implements Baz<String> {
    @Override
    public UnsolvedType doSomething(String value) {
        System.out.println("Foo is doing something with: " + value);
        return null;
    }

}
