package org.example;

import external.Anno;

public class Simple {

  @Anno(
      theClass = String.class,
      theArray = {1, 2},
      theString = "str",
      theInt = 5)
  private int x;

  @Anno private int y;

  public int target() {
    return x + y;
  }
}
