package com.example;

import org.example.Bar;
import org.example.Baz;
import org.example.Foo;

public class Simple {
  public void target(Foo f) {
    Bar b = f.get();
    Baz z = (Baz) f.get();
    System.out.println("" + b + z);
  }
}
