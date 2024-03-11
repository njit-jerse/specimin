package com.example;

import org.example.LambdaUser;

class Simple {
    // Target method.
    void bar(LambdaUser user) {
        user.use( (x, y, z) -> {
            System.out.println("x=" + x);
            System.out.println("y=" + y);
            System.out.println("z=" + z);
        } );
    }
}
