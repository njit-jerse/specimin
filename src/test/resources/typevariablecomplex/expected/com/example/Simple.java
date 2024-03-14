package com.example;

class Simple<I extends Simple<I>.Dog> {

    I shepherd;

    void bar() {
        shepherd.sound();
    }

    class Dog extends Animal {
    }
}

class Animal {

    public void sound() {
        throw new Error();
    }
}
