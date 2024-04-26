package com.example;

class Simple {

    void test(Foo f) {

        f.bar += 5;

        int y = 4;
        y += f.baz();

        long w = 1000000L;
        w -= f.qux;

        double d = 5.42;
        d *= f.razz();
    }
}
