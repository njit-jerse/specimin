package com.example;

class Simple {

    public <K, V> Iterator<Map.Entry<K, V>> iterator() {
        throw new java.lang.Error();
    }

    void bar() {
        Object iterator = iterator();
    }
}
