package org.example;

import external.Header;

public class Target {

  private final String PREFIX = "X";

  private final String HEADER_NAME = PREFIX + "-Header";

  @Header(HEADER_NAME)
  public void target() {}
}
