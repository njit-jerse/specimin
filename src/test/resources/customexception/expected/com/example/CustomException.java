package com.example;

public class CustomException extends java.lang.Exception {

    public CustomException(String msg) {
        throw new java.lang.Error();
    }
}
