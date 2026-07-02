package com.example;

import java.net.Socket;
import org.checkerframework.checker.mustcall.qual.Owning;

public class Simple {

    @Owning
    private Socket x;

    void bar() {
        x = new Socket();
    }
}
