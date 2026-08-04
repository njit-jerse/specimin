package com.example;

import java.util.List;
import org.example.Bar;
import org.example.Baz;
import org.example.Qux1;

public class Simple {
  public void target(Bar b) {
    Baz<? extends List<?>> x = b.get1();
    x = new Qux1();
    System.out.println(x);
  }
}
