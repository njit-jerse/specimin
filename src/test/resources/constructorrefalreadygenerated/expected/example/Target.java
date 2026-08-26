package example;

import library.Registry;
import library.Widget;

public final class Target {

    public void target() {
        Widget seeded = new Widget("seed");
        Registry.register(Widget::new);
        System.out.println(seeded);
    }
}
