package com.example;


public interface PropertyFactory<V, P extends WritableProperty<V>> {
    default P create(String name, PropertyTypeInfo<V> valueClass, PropertyMetadata<V> metadata) {
        throw new java.lang.Error();
    }
}
