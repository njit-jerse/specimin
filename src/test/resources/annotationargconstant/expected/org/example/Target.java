package org.example;

import external.Header;

public class Target {

  private static final String HEADER_NAME = "X-Header";

  @Header(HEADER_NAME)
  public void target() {}
}
