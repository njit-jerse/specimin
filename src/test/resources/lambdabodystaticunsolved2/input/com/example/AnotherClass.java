package com.example;

import static com.example.nullness.Nullness.castNonNull;

public class AnotherClass {

    public static AnotherClass distractor() {
        return castNonNull(null);
    }
}