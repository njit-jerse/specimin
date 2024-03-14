package com.example;

import java.util.Map;
import java.util.Set;

import com.example.primitives.Primitives;

public final class Simple<B> extends ForwardingMap<Class<? extends B>, B> {

    private static <B> Entry<Class<? extends B>, B> checkedEntry(final Entry<Class<? extends B>, B> entry) {
        return new ForwardingMapEntry<Class<? extends B>, B>() {

            protected Entry<Class<? extends B>, B> delegate() {
                return entry;
            }

            public B setValue(B value) {
                return super.setValue(cast(getKey(), value));
            }
        };
    }

    private static <B, T extends B> T cast(Class<T> type, B value) {
        return Primitives.wrap(type).cast(value);
    }
}