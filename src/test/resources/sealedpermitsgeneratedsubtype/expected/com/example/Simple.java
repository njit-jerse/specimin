package com.example;

import org.example.Foo;

public class Simple {

  public void target(Foo f) {
    Baz b = (Baz) f.get();
    SealedBase s = f.get();
    System.out.println(b + "" + s);
  }
}
