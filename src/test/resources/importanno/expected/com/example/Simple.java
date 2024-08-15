package com.example;

import org.checkerframework.checker.mustcall.qual.Owning;
import java.net.Socket;

public class Simple {

    @Owning
    private Socket x;

    void bar() {
        x = new Socket();
    }
}
