package com.example;

class Simple {
    /*@ ensures \result >= 5 */
    static int returnFive() {
        return 5;
    }

    /**
     * This method is used to check whether Specimin preserves comments using the OpenJML
     * modularity model.
     */
    /*@ \ensures \result >= 5 */
    static int test() {
        // both this comment and the above comment should be preserved
        int y = returnFive();
        return y;
    }
}