package com.example;

import java.util.IdentityHashMap;

class Simple<T, V> {

    protected IdentityHashMap<T, V> treeLookup;

    public boolean bar() {
        return treeLookup.isEmpty();
    }
}
