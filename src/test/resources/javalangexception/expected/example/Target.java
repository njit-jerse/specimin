package example;

import library.Listener;

public final class Target {

  public void target() {
    Listener listener =
        new Listener() {

          public void started() throws Exception {}
        };
    listener.toString();
  }
}
