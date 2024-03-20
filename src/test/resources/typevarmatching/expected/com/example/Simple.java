package com.example;

import org.testing.SimpleParent;

class Simple<E, V> extends SimpleParent<E, V> {

    public V get(E input) {
        System.out.println("Child method");
        return super.get(input);
    }
}
