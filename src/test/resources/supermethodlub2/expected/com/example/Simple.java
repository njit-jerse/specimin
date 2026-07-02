package com.example;

import org.example.Foo;

public class Simple {
  public void foo() {
    SomeChild child = new SomeChild();
    foo(child.method());
  }

  private void foo(Foo foo) {
    throw new java.lang.Error();
  }
}
