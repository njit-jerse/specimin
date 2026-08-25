package com.example;

import org.example.Bar;
import org.example.Baz;

public class Simple {
  public void target(Bar[][] bars) {
    Baz[][] b = (Baz[][]) bars;
    System.out.println(b);
  }
}
