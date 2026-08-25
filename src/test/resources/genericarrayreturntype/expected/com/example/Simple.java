package com.example;

import org.example.Bar;

public class Simple {

  Foo<?>[] handlers;

  void target() {
    handlers = Bar.baz();
  }
}
