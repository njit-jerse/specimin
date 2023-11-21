package com.example;

import org.checkerframework.specimin.CustomExceptionTest;

public class Simple {

    public void test() {
        try {
            throw new CustomException();
        } catch (CustomException e) {
            e.printStackTrace();
        }
    }
}
