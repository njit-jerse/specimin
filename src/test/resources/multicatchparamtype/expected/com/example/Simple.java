package com.example;

import org.example.Bar;

public class Simple {

  void target() {
    try {
      Bar.baz();
    } catch (IllegalStateException | NumberFormatException e) {
      Bar.record(e);
    }
  }
}
