package org.example;

import external.*;

public class Simple {
  @Anno(color = Color.RED)
  private int x;

  @Anno private int y;

  public int target() {
    return x + y;
  }
}
