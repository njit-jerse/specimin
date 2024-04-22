package com.example;

class Simple {

    int bar(Foo f) {
        if (f.isBar() || f.isBaz) {
            return 0;
        }

        if (f.qux() == 5) {
            return 1;
        }

        return 2;
    }
}
