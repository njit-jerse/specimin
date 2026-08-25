package com.example;

public class Simple {

  void foo(Anno anno) {
    sink(anno.velocity());
  }

  void sink(float value) {
    throw new java.lang.Error();
  }
}
