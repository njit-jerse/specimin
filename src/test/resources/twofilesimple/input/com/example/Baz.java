package com.example;

public class Baz {
    public Baz(String s) {

    }

    public static int addOne(int n) {
        return n + 1;
    }

    public Baz() {
        System.out.println("This constructor is never used, " +
                "so this ought to be removed by Specimin.");
    }
}
