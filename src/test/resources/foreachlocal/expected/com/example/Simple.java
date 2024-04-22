package com.example;

import java.util.List;

final class Simple {

    private static List<Foo> foos;

    public static void bar() {
        for (Foo f : foos) {
            if (f instanceof Bar) {
                f.doSomething();
            }
        }
    }
}
