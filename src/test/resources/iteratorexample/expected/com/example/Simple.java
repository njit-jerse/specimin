package com.example;

class Simple {

    public <K, V> Iterator<Map.Entry<K, V>> iterator() {
        throw new Error();
    }

    void bar() {
        Object iterator = iterator();
    }
}
