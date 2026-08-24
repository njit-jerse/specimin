package com.example;

import org.example.Bar;

public class Simple {

  private static class Foo<T> {}

  Foo<String>[] handlers;

  void target() {
    Bar.record(handlers);
  }
}
