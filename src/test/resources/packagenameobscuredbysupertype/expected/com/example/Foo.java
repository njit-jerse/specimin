package com.example;

import com.other.Unsolvable;

public class Foo extends Unsolvable {

  public Object bar() {
    return java.util.UUID.fromString("00000000-0000-0000-0000-000000000000");
  }
}
