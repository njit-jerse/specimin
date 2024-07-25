package com.example;

import org.example.Resource;

class Simple {

    void bar() throws Exception {
        try (Resource r = new Resource()) {
        }
    }
}
