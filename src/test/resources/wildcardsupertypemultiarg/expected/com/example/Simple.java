package com.example;

import org.example.Bar;
import org.example.Qux3;
import org.example.Two;

public class Simple {

  public void target(Bar b) {
    Two<?, ?> z = b.get3();
    z = new Qux3();
    System.out.println(z);
  }
}
