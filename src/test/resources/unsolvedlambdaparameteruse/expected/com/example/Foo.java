package com.example;

public class Foo {

  public void bar() {
    Bar.foo(
        (param) -> {
          int x = param.getX();
          int y = param.y;
        });
  }
}
