package com.example;

import org.testing.SimpleParent;

class Simple<E> extends SimpleParent<E> {
    public E get(E input) {
        System.out.println("Child method");
        return super.get(input);
    }
}
