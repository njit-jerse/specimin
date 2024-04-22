package com.example;

class Simple {

    int bar(Foo f) {
        if (f.isBar() || f.isBaz) {
            return 0;
        }
        return 1;
    }
}
