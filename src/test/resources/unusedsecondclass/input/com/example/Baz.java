package com.example;

public class Baz {
    public Baz(String s) {
        System.out.println("This constructor is never used, " +
                "so this ought to be removed by Specimin.");
    }

    public Baz() {
        System.out.println("This constructor is never used, " +
                "so this ought to be removed by Specimin.");
    }
}
