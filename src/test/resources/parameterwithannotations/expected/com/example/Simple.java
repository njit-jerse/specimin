package com.example;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.testing.UnsolvedType;

class Simple {

    void bar(byte @Nullable [] first, @Nullable UnsolvedType second) {
        throw new Error();
    }
}
