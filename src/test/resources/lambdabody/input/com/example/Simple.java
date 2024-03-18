package com.example;

import org.example.LambdaUser;
import com.example.Util.doSomething;

class Simple {
    // Target method.
    void bar(LambdaUser user) {
        user.use(x -> doSomething(x) );
    }
}
