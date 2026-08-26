package example;

import library.Listener;

public final class Target {
  public void target() {
    Listener listener =
        new Listener() {
          @Override
          public void started(String name) {}
        };
    listener.toString();
  }
}
