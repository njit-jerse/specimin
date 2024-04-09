package com.example;

import java.util.Collection;
import org.checkerframework.checker.nullness.qual.Initialized;

class Simple {
    void <T> bar(Collection<@Initialized ? extends T>) {
    }
}
