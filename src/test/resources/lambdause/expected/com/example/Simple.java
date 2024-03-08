package com.example;

import org.example.LambdaUser;

class Simple {
    void bar() {
        LambdaUser.use(x -> {
            System.out.println(x);
        });
    }
}
