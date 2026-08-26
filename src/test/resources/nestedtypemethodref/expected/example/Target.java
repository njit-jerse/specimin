package example;

import java.util.function.Supplier;
import library.Outer;

public final class Target {

    public void target() {
        Supplier<String> s = Outer.Nested::foo;
        s.get();
    }
}
