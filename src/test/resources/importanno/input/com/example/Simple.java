package com.example;

import org.checkerframework.checker.mustcall.qual.Owning;

import java.net.Socket;

public class Simple {

    @Owning
    private Socket x;

    // Target method.
    void bar() {
        x = new Socket();
    }
}
