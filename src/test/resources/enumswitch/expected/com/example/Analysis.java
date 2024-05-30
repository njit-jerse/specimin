package com.example;

public interface Analysis {

    enum Direction {

        FORWARD, BACKWARD
    }

    default Direction getDirection() {
        throw new Error();
    }
}