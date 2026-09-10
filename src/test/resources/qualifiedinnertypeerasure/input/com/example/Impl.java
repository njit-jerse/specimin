package com.example;

import com.other.Missing;

public class Impl implements Rule {
    @Override
    public String apply(Outer<String>.Inner<Integer> x, Missing y) {
        return x.describe();
    }
}
