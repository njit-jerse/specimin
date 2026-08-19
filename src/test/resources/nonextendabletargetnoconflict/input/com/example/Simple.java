package com.example;

import org.example.Item;

class Simple {
    // Target method.
    <U> void bar(Item item) {
        int i = item.getInt();
        String[] a = item.getArray();
        Color c = item.getColor();
        Point p = item.getPoint();
        U u = item.getU();
    }
}
