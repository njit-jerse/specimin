package com.example;

import org.example.Resource;

class Simple {

    void bar() {
        try (Resource r = new Resource()) {
        }
    }
}
