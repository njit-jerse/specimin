package com.example;

class Simple<I extends Simple<I>.Dog> {
    I shepherd;
    void bar() {
        shepherd.sound();
    }
    class Dog extends Animal {
        void walk() {
            throw new Error();
        }
    }
}

class Animal {
    public void sound() {
        System.out.println("No sound");
    }
}
