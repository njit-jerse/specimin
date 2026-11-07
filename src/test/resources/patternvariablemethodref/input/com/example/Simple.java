package com.example;

import org.example.Foo;

class Simple {
  void bar(Object o) {
    if (o instanceof Foo f) {
      Runnable r = f::baz;
    }
  }
}
