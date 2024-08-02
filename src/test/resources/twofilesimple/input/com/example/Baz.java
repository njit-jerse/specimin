package com.example;

public class Baz {
    public Baz(String s) {

    }

    public Baz() {
        System.out.println("This constructor is never used, " +
                "so this ought to be removed by SpecSlice.");
    }
}
