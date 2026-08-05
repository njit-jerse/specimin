package com.example;

import org.example.Foo;
import org.example.Bar;
import org.example.Baz;
import org.example.Qux;

public class Simple {
  public void target(Bar b) {
    Baz<? super Foo> y = b.get2();
    // This assignment implies that Foo <: Qux, because of the bound on the wildcard in y's static type.
    y = new Baz<Qux>();
  }
}
