package com.example;

class Foo implements Baz<String> {
    @Override
    public void doSomething(String value) {
        System.out.println("Foo is doing something with: " + value);
    }

}
