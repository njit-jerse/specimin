package com.example;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;

class Simple<K extends Object, V extends Object> extends AbstractMap<K, V> {
    void bar() {
    }

    public Set<Map.Entry<K, V>> entrySet() {
        throw new java.lang.Error();
    }
}
