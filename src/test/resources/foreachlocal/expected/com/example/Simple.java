package com.example;

import java.util.List;

final class Simple {

    List<Foo> foos;

    public static void bar() {
        for (Foo f : foos) {
            if (f instanceof Bar) {
                f.doSomething();
            }
        }
    }
}
