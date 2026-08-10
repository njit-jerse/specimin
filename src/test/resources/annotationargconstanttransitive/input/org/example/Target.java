package org.example;

import external.Header;

public class Target {
  private static final String PREFIX = "X";
  private static final String HEADER_NAME = PREFIX + "-Header";

  @Header(HEADER_NAME)
  public void target() {}
}
