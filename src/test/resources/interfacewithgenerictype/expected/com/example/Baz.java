package com.example;

public interface Baz<T> {

    void doSomething(T value);

    void doSomething(int x);
}
