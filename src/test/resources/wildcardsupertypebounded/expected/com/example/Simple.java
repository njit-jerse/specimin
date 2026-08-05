package com.example;

import org.example.Bar;
import org.example.Baz;
import org.example.Qux1;
import org.example.Qux2;
import org.example.Thing;

public class Simple {

  public void target(Bar b) {
    Baz<? extends Thing> x = b.get1();
    x = new Qux1();
    Baz<? super Thing> y = b.get2();
    y = new Qux2();
    System.out.println("" + x + y);
  }
}
