package com.example;

public class Simple<K, V> {
    public static void bar(AbstractMapEntry<K, V> entry) {
        entry.getKey();
    }
}