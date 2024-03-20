package com.example;

import org.testing.SimpleParent;

class Simple extends SimpleParent {
    SimpleParent parent;
    public void get() {
        parent.get();
    }
}
