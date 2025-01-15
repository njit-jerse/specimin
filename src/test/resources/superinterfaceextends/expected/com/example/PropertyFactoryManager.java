package com.example;

public interface PropertyFactoryManager {

    @SuppressWarnings({ "rawtypes", "unchecked" })
    default <V, P extends ReadableProperty<V>> P create(Class<P> propertyType, Class<V> valueClass) {
        PropertyFactory factory = getRequiredFactory(propertyType, valueClass);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    default <V, P extends ReadableProperty<V>> PropertyFactory<V, ? extends P> getRequiredFactory(Class<P> propertyType, Class<V> valueType) {
        throw new java.lang.Error();
    }
}
