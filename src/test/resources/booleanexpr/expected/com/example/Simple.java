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

        return 2;
    }
}
