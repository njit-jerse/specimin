package com.example;

import org.example.Resource;
import org.example.OtherResource;
import org.example.ThirdResource;

class Simple {

    private final ThirdResource r = null;

    void bar(final OtherResource o) throws Exception {
        try (Resource r = new Resource()) {
        }
        try (o) {
        }
        try (r) {
        }
    }
}
