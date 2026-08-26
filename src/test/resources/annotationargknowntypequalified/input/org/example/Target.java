package org.example;

import external.Constants;

@Header(Constants.HEADER_NAME)
public class Target {
  public void target(String s) {
    if (s == null) {
      throw new IllegalArgumentException();
    }
  }
}
