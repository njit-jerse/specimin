package com.example;

abstract class AbstractMapEntry<K, V> {

    public V setValue(V value) {
        throw new java.lang.Error();
    }

    public K getKey() {
        throw new java.lang.Error();
    }
}