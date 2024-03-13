package com.example;

import java.util.Set;
import java.util.Collection;

import com.example.WrappedSet;

public class Simple<K, V> {
    // Target method.
    Collection<V> bar(K Key, Collection<V> collection) {
        return new WrappedSet(key, (Set<V>) collection);
    }
}
