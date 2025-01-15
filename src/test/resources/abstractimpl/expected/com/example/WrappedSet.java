package com.example;

import java.util.Set;
import java.util.Iterator;
import java.util.Collection;

class WrappedSet<K, V> implements Set<V> {

    WrappedSet(K key, Set<V> delegate) {
        throw new java.lang.Error();
    }

    public int size() {
        throw new java.lang.Error();
    }

    public Iterator<V> iterator() {
        throw new java.lang.Error();
    }

    public void clear() {
        throw new java.lang.Error();
    }

    public boolean remove(Object o) {
        throw new java.lang.Error();
    }

    public boolean removeAll(Collection<?> c) {
        throw new java.lang.Error();
    }

    public boolean retainAll(Collection<?> c) {
        throw new java.lang.Error();
    }

    public boolean addAll(Collection<? extends V> collection) {
        throw new java.lang.Error();
    }

    public boolean contains(Object o) {
        throw new java.lang.Error();
    }

    public boolean containsAll(Collection<?> c) {
        throw new java.lang.Error();
    }

    public boolean add(V value) {
        throw new java.lang.Error();
    }

    public <T> T[] toArray(T[] type) {
        throw new java.lang.Error();
    }

    public Object[] toArray() {
        throw new java.lang.Error();
    }

    public boolean isEmpty() {
        throw new java.lang.Error();
    }
}