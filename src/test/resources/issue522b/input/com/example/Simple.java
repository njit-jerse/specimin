package com.example;

import org.example.Bar;

public class Simple {
  Foo<String>[] handlers;

  Simple(Foo<String>[] in) {
    handlers = in;
    Bar.baz();
  }
}
