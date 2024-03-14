package com.example;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public abstract class ForwardingMap<K, V> extends ForwardingObject implements Map<K, V> {

    public int size() {
        throw new Error();
    }

    public boolean isEmpty() {
        throw new Error();
    }

    public V remove(Object object) {
        throw new Error();
    }

    public void clear() {
        throw new Error();
    }

    public boolean containsKey(Object key) {
        throw new Error();
    }

    public boolean containsValue(Object value) {
        throw new Error();
    }

    public V get(Object key) {
        throw new Error();
    }

    public V put(K key, V value) {
        throw new Error();
    }

    public void putAll(Map<? extends K, ? extends V> map) {
        throw new Error();
    }

    public Set<K> keySet() {
        throw new Error();
    }

    public Collection<V> values() {
        throw new Error();
    }

    public Set<Entry<K, V>> entrySet() {
        throw new Error();
    }
}
