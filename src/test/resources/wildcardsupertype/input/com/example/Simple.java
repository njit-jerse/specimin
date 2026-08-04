package com.example;

import org.example.Bar;
import org.example.Baz;
import org.example.Qux;

public class Simple {
  public void target(Bar b) {
    Baz<?> x = b.get();
    x = new Qux();
    System.out.println(x);
  }
}
