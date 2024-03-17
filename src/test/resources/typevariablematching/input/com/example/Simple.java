package com.example;

import org.testing.Baz;

class Simple<E>  {
    Baz<E> field;

    public E bar() {
        return field.bar();
    }
}
