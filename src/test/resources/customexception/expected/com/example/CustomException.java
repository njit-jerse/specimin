package com.example;

class CustomException extends Exception {

    public CustomException() {
        throw new Error();
    }
}
