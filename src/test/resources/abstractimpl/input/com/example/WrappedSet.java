package com.example;

import java.util.Set;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Collection;

class WrappedSet<K, V> implements Set<V> {

    WrappedSet(K key, Set<V> delegate) {
        throw new java.lang.Error();
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public boolean equals(Object object) {
        return true;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public String toString() {
        return "hello world";
    }

    @Override
    public Iterator<V> iterator() {
        return null;
    }

    @Override
    public Spliterator<V> spliterator() {
        return null;
    }

    @Override
    public void clear() {

    }

    @Override
    public boolean remove(Object o) {
        return false;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return false;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return false;
    }

    @Override
    public boolean addAll(Collection<? extends V> collection) {
        return false;
    }

    @Override
    public boolean contains(Object o) {
        return false;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return false;
    }

    @Override
    public boolean add(V value) {
        return false;
    }

    @Override
    public <T> T[] toArray(T[] type) {
        return null;
    }

    @Override
    public Object[] toArray() {
        return null;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }
}
