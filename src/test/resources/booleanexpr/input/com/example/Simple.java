package com.example;

class Simple {

    int bar(Foo f) {
        if (f.isBar() || f.isBaz && f.p) {
            return 0;
        }

        if (f.qux() == 5) {
            return 1;
        }

        if (f.razz() != 5.4) {
            return 2;
        }

        int y = f.getX() + 2;
        long z = f.getLong() * 3L;
        Long w = Long.valueOf(100L);
        Long x = f.getBigLong() / w;

        // Note: this test wouldn't work, because javac doesn't issue
        // an error about string concatenation with non-strings (everything
        // has a toString() method!).
        // String s = f.getS() + "!";

        return 2;
    }
}
