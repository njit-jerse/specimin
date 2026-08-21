package com.example;

import org.example.Item;

class Simple {
    // Target method.
    void bar(Item item) {
        item.getPayload().foo();
        Object o = item.getPayload();
    }
}
