package com.example;

import org.example.Baz;
import org.example.Foo;

public class Simple {

  public void target(Foo f) {
    int x = f.get();
    Baz b = (Baz) f.get();
    System.out.println(b + "" + x);
  }
}
