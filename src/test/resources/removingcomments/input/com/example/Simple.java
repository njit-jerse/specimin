package com.example;

class Simple {
    static int returnFive() {
        return 5;
    }
    static int returnTen() {
        return returnFive() * 2;
    }

    /**
     * This method is used to test if the comments (including Javadoc) are all removed by SpecSlice
     */
    static void test() {
        // the comment here should be removed
        int y = returnTen();
        Simple s = new Simple();
    }
}
