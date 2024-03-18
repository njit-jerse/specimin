package com.example;

import org.testing.Baz;

class Simple<E> {

    public Baz<E> bar() {
        return new Baz<E>() {
            void add(E add) {
                throw new RuntimeException();
            }
        };
    }
}
