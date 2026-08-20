package com.example;

import org.example.Item;

class Simple {
    // Target method.
    void bar(Item item) {
        Color c = item.get();
        Payload p = item.get();
    }
}
