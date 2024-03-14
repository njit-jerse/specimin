package com.example;

import java.util.Map;
import java.util.Map.Entry;

public abstract class ForwardingMapEntry<K, V> extends ForwardingObject implements Map.Entry<K, V> {

    protected ForwardingMapEntry() {
        throw new Error();
    }

    protected abstract Entry<K, V> delegate();

    public K getKey() {
        throw new Error();
    }

    public V getValue() {
        throw new Error();
    }

    public V setValue(V value) {
        throw new Error();
    }
}
