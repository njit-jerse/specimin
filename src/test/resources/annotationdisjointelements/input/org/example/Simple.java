package org.example;

import external.Anno;

public class Simple {

  @Anno(number = 1)
  private int x;

  @Anno(text = "s")
  private int y;

  public int target() {
    return x + y;
  }
}
