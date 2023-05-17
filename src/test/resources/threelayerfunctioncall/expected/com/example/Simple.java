package com.example;

class Simple {
    static int returnTen() {
        throw new Error();
    }
    static void test() {
        int y = returnTen();
        Simple s = new Simple();
    }
}
