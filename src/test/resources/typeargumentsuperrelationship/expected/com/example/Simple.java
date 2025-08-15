package com.example;

public class Simple {

  void bar() {
    Foo<? extends Bar> foo = new Foo<Baz>();
    Foo2<? extends Bar2> foo2 = new Foo2<Baz2>();
    foo = foo2;
  }
}
