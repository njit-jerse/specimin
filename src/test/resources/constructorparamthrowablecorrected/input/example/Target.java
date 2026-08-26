package example;

import library.UnsolvedException;
import library.Widget;

public final class Target {
    public void target() {
        try {
            Widget.risky();
        } catch (UnsolvedException e) {
            System.out.println(new Widget(e.getMessage()));
        }
    }
}
