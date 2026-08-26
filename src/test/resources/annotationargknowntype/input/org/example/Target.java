package org.example;

import static external.Constants.HEADER_NAME;

@Header(HEADER_NAME)
public class Target {
  public void target(String s) {
    if (s == null) {
      throw new IllegalArgumentException();
    }
  }
}
