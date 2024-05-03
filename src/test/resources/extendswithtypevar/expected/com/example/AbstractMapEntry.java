package com.example;

import java.util.Map.Entry;

abstract class AbstractMapEntry<K, V> extends Entry<K, V> {

    public V setValue(V value) {
        throw new Error();
    }

    public K getKey() {
        throw new Error();
    }

    public boolean equals(Object object) {
        throw new Error();
    }

    public int hashCode() {
        throw new Error();
    }
}