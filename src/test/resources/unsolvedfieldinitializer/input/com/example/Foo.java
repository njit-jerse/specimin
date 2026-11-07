package com.example;

import androidx.collection.ArrayMap;
import java.util.Map;
import other.Unsolved;

public class Foo {
  private final Map<String, String> defaults = new ArrayMap<>();

  public void build(Unsolved u) {
    System.out.println(defaults);
  }
}
