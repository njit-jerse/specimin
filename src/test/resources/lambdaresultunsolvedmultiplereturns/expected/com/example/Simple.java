package com.example;

import java.util.function.Supplier;

import org.example.Item;
import org.example.Other;

class Simple {

    Supplier<String> bar(Item item, Other other, boolean flag) {
        return () -> {
            if (flag) {
                return item.get();
            }
            return other.get();
        };
    }
}
