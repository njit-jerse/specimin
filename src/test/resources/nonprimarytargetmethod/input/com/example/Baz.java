package com.example;

class Baz {
    int toBeRemovedField;
    void toBeRemovedMethod() {
        System.out.println("Will be removed!");
    }
}

class NonPrimary {
    static void printMessage() {
        System.out.println("This method will be preserved");
    }
}