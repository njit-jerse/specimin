package com.example;

public class CustomException extends Exception {

    public CustomException(String msg) {
        throw new java.lang.Error();
    }
}
