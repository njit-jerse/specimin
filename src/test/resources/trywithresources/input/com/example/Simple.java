package com.example;

import org.example.Resource;

class Simple {
    // Target method.
    void bar() throws Exception {
        try (Resource r = new Resource()) {
            // do something
        }
    }
}
