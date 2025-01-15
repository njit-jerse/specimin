package com.example;

class BigSimple {
    public void printMessage() {
        throw new java.lang.Error();
    }
}

public class Simple extends BigSimple {
    public void printMessage() {
        super.printMessage();
        BigSimple obj = new BigSimple();
    }
}
