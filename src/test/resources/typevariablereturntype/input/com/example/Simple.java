package com.example;

import org.example.Bar;

public class Simple<T> {
  T field;

  void target() {
    field = Bar.baz();
  }
}
