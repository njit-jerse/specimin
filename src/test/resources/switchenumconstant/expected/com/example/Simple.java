package com.example;

import org.example.MyEnum;

class Simple {
    void bar(MyEnum m) {
        switch(m) {
            case CONSTANT1:
                int x = 5 + 8;
                break;
            case CONSTANT2:
                int y = 3 * 2;
                break;
        }
        int z = switch (m) {
            case CONSTANT3 -> 5;
            case CONSTANT4 -> 6;
            default -> 0;
        };
    }
}
