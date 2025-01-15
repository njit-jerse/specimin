package com.example;

public class Simple<K> {

    AccessOrderDeque<K> bar() {
        throw new java.lang.Error();
    }

    void foo() {
        AccessOrderDeque<K> x = bar();
    }
}
