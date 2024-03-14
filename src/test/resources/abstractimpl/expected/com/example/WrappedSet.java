package com.example;

import java.util.Set;
import java.util.Iterator;
import java.util.Collection;

class WrappedSet<K, V> implements Set<V> {

    WrappedSet(K key, Set<V> delegate) {
        throw new Error();
    }

    public int size() {
        throw new Error();
    }

    public Iterator<V> iterator() {
        throw new Error();
    }

    public void clear() {
        throw new Error();
    }

    public boolean remove(Object o) {
        throw new Error();
    }

    public boolean removeAll(Collection<?> c) {
        throw new Error();
    }

    public boolean retainAll(Collection<?> c) {
        throw new Error();
    }

    public boolean addAll(Collection<? extends V> collection) {
        throw new Error();
    }

    public boolean contains(Object o) {
        throw new Error();
    }

    public boolean containsAll(Collection<?> c) {
        throw new Error();
    }

    public boolean add(V value) {
        throw new Error();
    }

    public <T> T[] toArray(T[] type) {
        throw new Error();
    }

    public Object[] toArray() {
        throw new Error();
    }

    public boolean isEmpty() {
        throw new Error();
    }
}