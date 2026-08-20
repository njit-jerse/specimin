package com.example;

import org.example.Item;

class Simple {
    // Target method.
    void bar(Item item) {
        Animal a = item.get();
        Dog d = item.get();
        System.out.println(d + "" + a);
    }
}
