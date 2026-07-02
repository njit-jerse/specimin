package com.example;

import java.util.Map.Entry;

abstract class AbstractMapEntry<K, V> implements Entry<K, V> {

    public V setValue(V value) {
        throw new java.lang.Error();
    }

    public K getKey() {
        throw new java.lang.Error();
    }
}