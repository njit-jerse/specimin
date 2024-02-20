 package com.example;

class Simple {
    // a should be removed, b should be simplified, and c should have its initializer removed.
    int a = 0;
    final int b = returnTwo() + 1;
    int c = returnTwo();
    private static int returnTwo() {
        return 2;
    }
    void test() {
        c++;
        int d = b + 1;
    }
}
