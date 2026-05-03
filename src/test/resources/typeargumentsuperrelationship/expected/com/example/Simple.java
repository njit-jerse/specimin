package com.example;

public class Simple {

  void bar() {
    Foo<? extends Bar> foo = new Foo<Baz>();
    Foo<? extends Bar2> foo2 = new Foo<Baz2>();
    BadFoo<? extends Bar3> badFoo = new BadFoo<Baz3>();
    foo = foo2;
    badFoo = new Foo<>();
  }
}
