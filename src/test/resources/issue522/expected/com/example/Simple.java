package com.example;

import org.example.Bar;

public class Simple {

  Foo<?>[] handlers;

  Simple(Foo<?>[] in) {
    handlers = in;
    Bar.baz();
  }
}
