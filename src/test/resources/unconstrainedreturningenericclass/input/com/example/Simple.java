package com.example;

import org.example.Baz;
import org.example.Foo;

public class Simple {
  public void target(Foo<Baz> f) {
    Baz b = (Baz) f.get();
    String s = f.get();
    System.out.println(b + s);
  }
}
