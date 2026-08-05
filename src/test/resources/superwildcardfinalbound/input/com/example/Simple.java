package com.example;

import org.example.Bar;
import org.example.Baz;
import org.example.Qux2;

public class Simple {
  public void target(Bar b) {
    Baz<? super String> y = b.get2();
    y = new Qux2();
    System.out.println(y);
  }
}
