// test case from Guava. Mostly, this test is here to check that
// we don't create extraneous classes for type variables.

package com.example;

import java.util.Map.Entry;

abstract class AbstractMapEntry<K, V> implements Entry<K, V> {

    public V setValue(V value) {
        throw new java.lang.Error();
    }

    public K getKey() {
        return delegate().getKey();
    }

    public boolean equals(Object object) {
        throw new java.lang.Error();
    }

    public int hashCode() {
        throw new java.lang.Error();
    }
}