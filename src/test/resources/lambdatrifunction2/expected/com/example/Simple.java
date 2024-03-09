package com.example;

import org.example.LambdaUser;

class Simple {
    void bar(LambdaUser user) {
        user.use( (x, y, z) -> {
            return null;
        } );
    }
}
