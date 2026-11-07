package com.example;

import java.util.Map;
import other.Unsolved;

public class Foo {

  private final Map<String, String> defaults = null;

  public void build(Unsolved u) {
    System.out.println(defaults);
  }
}
