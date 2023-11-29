package com.example;

import org.checkerframework.specimin.CustomExceptionTest;

public class Simple {

    public void test() {
        try {
            throw new CustomException("dummy");
        } catch (CustomException e) {
            e.printStackTrace();
        }
    }
}
