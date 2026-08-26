package example;

import library.Outer;

public final class Target {

    private Outer.Nested nested;

    private library.Outer.Nested alsoNested;

    private Outer.Mid.Deeper deeper;

    public void target() {
        nested = new Outer.Nested();
        alsoNested = new library.Outer.Nested();
        deeper = new Outer.Mid.Deeper();
    }
}
