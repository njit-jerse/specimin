package com.example;

import com.other.Missing;

public class Target {
    String target(Outer<String>.Inner<Integer> x, Missing y) {
        Impl impl = new Impl();
        Rule other = (a, b) -> "lambda";
        return impl.apply(x, y) + other.hashCode();
    }
}
