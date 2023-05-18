package com.example;

class BigSimple {
    public void printMessage() {}
}

public class Simple extends BigSimple {
    @Override
    public void printMessage() {
        super.printMessage();
        BigSimple obj = new BigSimple();
    }
}
