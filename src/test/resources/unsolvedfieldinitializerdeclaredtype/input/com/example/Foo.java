package com.example;

import androidx.collection.ArrayMap;
import other.Unsolved;

public class Foo {
  private final ArrayMap<String, String> defaults = new ArrayMap<>();

  public void build(Unsolved u) {
    System.out.println(defaults);
  }
}
