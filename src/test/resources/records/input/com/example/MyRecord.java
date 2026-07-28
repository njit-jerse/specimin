package com.example;

public record MyRecord(String str, Foo foo, int x, boolean unusedVariableToRemove) {
    public MyRecord(String str, Foo foo) {
        this(str, foo, 0, false);
    }

    public MyRecord() {
        this("", null);
    }
}