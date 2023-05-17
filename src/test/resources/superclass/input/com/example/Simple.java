package com.example;

class BigSimple {
    public void printMessage() {
        String x = "Hello";
    }
}

public class Simple extends BigSimple {
    @Override
    public void printMessage() {
        super.printMessage();
        String y = "It's me";
    }
}
