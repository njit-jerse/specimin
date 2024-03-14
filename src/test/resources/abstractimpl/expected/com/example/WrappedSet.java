package com.example;

import java.util.Set;
import java.util.Iterator;
import java.util.Collection;

class WrappedSet<K, V> implements Set<V> {

    WrappedSet(K key, Set<V> delegate) {
        throw new Error();
    }

    @Override
    public int size() {
        throw new Error();
    }

    @Override
    public Iterator<V> iterator() {
        throw new Error();
    }

    @Override
    public void clear() {
        throw new Error();
    }

    @Override
    public boolean remove(Object o) {
        throw new Error();
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new Error();
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new Error();
    }

    @Override
    public boolean addAll(Collection<? extends V> collection) {
        throw new Error();
    }

    @Override
    public boolean contains(Object o) {
        throw new Error();
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        throw new Error();
    }

    @Override
    public boolean add(V value) {
        throw new Error();
    }

    @Override
    public <T> T[] toArray(T[] type) {
        throw new Error();
    }

    @Override
    public Object[] toArray() {
        throw new Error();
    }

    @Override
    public boolean isEmpty() {
        throw new Error();
    }
}