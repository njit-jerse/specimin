package com.example;

abstract class AbstractMapEntry<K, V> {

    public V setValue(V value) {
        throw new Error();
    }

    public K getKey() {
        throw new Error();
    }
}