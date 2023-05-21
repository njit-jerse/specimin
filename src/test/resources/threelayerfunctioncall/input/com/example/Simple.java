package com.example;

class Simple {
    static int returnFive() {
        return 5;
    }
    static int returnTen() {
        return returnFive() * 2;
    }
    static void test() {
        int y = returnTen();
        Simple s = new Simple();
    }
}
