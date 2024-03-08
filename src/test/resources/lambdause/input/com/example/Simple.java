package com.example;

import org.example.LambdaUser;

class Simple {
    // Target method.
    void bar() {
        LambdaUser.use(x -> { System.out.println(x); } );
    }
}
