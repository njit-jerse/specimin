package com.example;

import org.testing.Baz;
import org.testing.Foo;

public class Simple {

    public Baz getBaz (){
        return new Baz() {
            private Foo foo = new Foo("starting");

            public void remove() {
                foo.remove();
            }
        };
    }
}
