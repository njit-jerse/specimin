package com.example;

class BigSimple {
    public void printMessage() {
        throw new Error();
    }
}

public class Simple extends BigSimple {
    @Override
    public void printMessage() {
        super.printMessage();
        BigSimple obj = new BigSimple();
    }
}
