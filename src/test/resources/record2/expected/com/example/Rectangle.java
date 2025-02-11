package com.example;

record Rectangle(double len, double width) {

    public static void bar() {
        Rectangle r = new Rectangle(10, 6).rotate();
    }

    public Rectange rotate() {
        throw new java.lang.Error();
    }
}