package com.example;

import java.util.Collection;
import org.checkerframework.checker.nullness.qual.Initialized;

class Simple {
    // Target method.
    void <T> bar(Collection<@Initialized ? extends T>) {
        // Nothing to do.
    }
}
