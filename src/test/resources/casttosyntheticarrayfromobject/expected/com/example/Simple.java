package com.example;

import org.example.Baz;

public class Simple {

  public void target(Object o) {
    Baz[] b = (Baz[]) o;
    System.out.println(b);
  }
}
