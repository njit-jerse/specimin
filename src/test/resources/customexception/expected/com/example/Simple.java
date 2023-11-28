package com.example;

public class Simple {

    public void test() {
        try {
            throw new CustomException("dummy");
        } catch (CustomException e) {
            e.printStackTrace();
        }
    }
}
