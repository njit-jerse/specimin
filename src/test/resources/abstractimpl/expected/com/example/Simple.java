package com.example;

import java.util.Collection;
import java.util.Set;

public class Simple<K, V> {
    Collection<V> bar(K key, Collection<V> collection) {
        return new WrappedSet(key, (Set<V>) collection);
    }
}
