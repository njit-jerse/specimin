package example;

import library.Outer.Nested;

public final class Target {

    private Nested nested;

    public void target() {
        nested = new Nested();
    }
}
