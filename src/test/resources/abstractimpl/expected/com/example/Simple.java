package com.example;

import java.util.Set;
import java.util.Collection;

public class Simple<K, V> {
    Collection<V> bar(K key, Collection<V> collection) {
        return new WrappedSet(key, (Set<V>) collection);
    }
}
