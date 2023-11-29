package com.example;

import org.checkerframework.specimin.CustomExceptionTest;

public class Simple {
    public void test() {
        try {
            throw new CustomException("It's an unsolvable custom exception");
        } catch (CustomException e) {
            e.printStackTrace();
        }
    }
}