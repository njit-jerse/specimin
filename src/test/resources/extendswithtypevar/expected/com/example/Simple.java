package com.example;

public class Simple<K, V> {
    public void bar(AbstractMapEntry<K, V> entry) {
        entry.getKey();
    }
}