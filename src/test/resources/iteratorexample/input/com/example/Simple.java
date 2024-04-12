package com.example;

class Simple {

    public Iterator<Map.Entry<K,V>> iterator() {
        return new Iterator<Map.Entry<K,V>>() {
            private final Iterator<? extends Map.Entry<? extends K, ? extends V>> i = c.iterator();

            public boolean hasNext() {
                return i.hasNext();
            }
            public Map.Entry<K,V> next() {
                return new UnmodifiableEntry<>(i.next());
            }
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    void bar() {
        // This is the target method. This test is just checking that the blob
        // above doesn't cause a crash. The blob above must be used to trigger the crash.
        Object iterator = iterator();
    }
}
