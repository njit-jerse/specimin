package com.example;

public class Simple {
    public void test() {
        try {
            throw new CustomException("It's an unsolvable custom exception");
        } catch (CustomException e) {
            e.printStackTrace();
        }
    }
}