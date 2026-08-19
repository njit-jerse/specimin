package com.example;

import org.example.Item;

class Simple {
    // Target method.
    void bar(Item item) {
        Point pt = item.get();
        Payload p = item.get();
    }
}
