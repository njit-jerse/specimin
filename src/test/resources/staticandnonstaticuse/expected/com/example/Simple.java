package com.example;

import org.example.Foo;
import org.example.Bar;

public class Simple {

    void bar(boolean b) {
        final Foo foo = new Foo();
        if (b) {
            Foo.setThis(new Bar());
        } else {
            Bar b1 = null;
            Foo.setThis(b1);
        }
    }
}
