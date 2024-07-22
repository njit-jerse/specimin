package com.example;

import java.util.Collection;
import org.checkerframework.checker.nullness.qual.*;
import org.checkerframework.checker.initialization.qual.Initialized;

class Simple {
    public static <T extends @Initialized  @Nullable @UnknownKeyFor Object>  @Initialized  @NonNull @UnknownKeyFor Collection<T> unmodifiableCollection(@Initialized  @NonNull @UnknownKeyFor Collection<@Initialized  @KeyForBottom @NonNull ? extends T> c) {
        return null;
    }
}

