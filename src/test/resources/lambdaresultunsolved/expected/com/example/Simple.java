package com.example;

import java.util.function.Supplier;

import org.example.Item;

class Simple {

    void bar(Item item) {
        Supplier<String> supplier = () -> { return item.getPayload(); };
    }
}
