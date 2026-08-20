package com.example;

import org.example.Baz;
import org.example.Foo;

public class Simple {
  public void target(Foo f) {
    Baz b = (Baz) f.get();
    int x = f.get();
    System.out.println(b + "" + x);
  }
}
