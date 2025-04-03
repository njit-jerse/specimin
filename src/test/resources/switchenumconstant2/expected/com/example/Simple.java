package com.example;

import org.example.MyEnum;

class Simple extends Foo {
    void bar(MyEnum m) {
        switch(m) {
            case CONSTANT1:
                int x = CONSTANT5;
                break;
            case CONSTANT2:
                int y = 3 * 2;
                break;
        }
        int z = switch (m) {
            case CONSTANT3 -> CONSTANT6;
            case CONSTANT4 -> 6;
            default -> 0;
        };
    }
}
