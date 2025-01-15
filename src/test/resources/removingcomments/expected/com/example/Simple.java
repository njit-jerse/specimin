package com.example;

class Simple {

    static int returnTen() {
        throw new java.lang.Error();
    }

    static void test() {
        int y = returnTen();
        Simple s = new Simple();
    }
}
