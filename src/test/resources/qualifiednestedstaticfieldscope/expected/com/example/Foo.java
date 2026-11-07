package com.example;

import com.other.Outer;

public class Foo {
  public void bar() {
    Outer.Inner.UP.foo();
  }
}
