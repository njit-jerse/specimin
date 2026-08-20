package com.example;

import org.example.Baz;
import org.example.Foo;

public class Simple {

  public void target(Foo f) {
    SealedBase s = f.get();
    Baz b = (Baz) f.get();
    System.out.println(b + "" + s);
  }
}
