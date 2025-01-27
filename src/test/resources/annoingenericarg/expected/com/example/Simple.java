package com.example;

import java.util.Set;
import org.checkerframework.checker.nullness.KeyFor;

class Simple<K> {

    public Set<@KeyFor("this") K> bar() {
        return null;
    }
}
