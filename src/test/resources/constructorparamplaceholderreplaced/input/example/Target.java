package example;

import library.Helper;
import library.Widget;

public final class Target {
    public void target() {
        Widget widget = new Widget(Helper.make());
        String actual = Helper.make();
        System.out.println(widget + actual);
    }
}
