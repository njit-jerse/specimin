package com.example;

public record MyRecord(String str, Foo foo, int x) {
    public MyRecord(String str, Foo foo) {
        this(null, null, 0);
    }

    public MyRecord() {
        this(null, null, 0);
    }
}