package com.example;

import org.example.LambdaUser;

class Simple {

    void bar(LambdaUser user) {
        user.use(x -> x == null ? doSomething(x) : x );
    }
}
