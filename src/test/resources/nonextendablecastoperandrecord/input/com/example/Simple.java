package com.example;

import org.example.Baz;
import org.example.Foo;

public class Simple {
  public void target(Foo f) {
    Baz b = (Baz) f.get();
    Point p = f.get();
    System.out.println(b + "" + p);
  }
}
