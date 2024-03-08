package com.example;

import org.example.LambdaUser;

class Simple {
    // Target method.
    void bar(LambdaUser user) {
        user.use( (x, y) -> new Object() );
    }
}
