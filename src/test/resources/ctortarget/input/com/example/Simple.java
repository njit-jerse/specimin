package com.example;

class Simple {
    private Simple(Foo foo, int x) {
        throw new Error();
    }
}
