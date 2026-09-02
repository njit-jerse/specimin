package com.example;

import external.Unsolved;

public class Foo {

  void foo() {
    try {
      MyOtherRecord other = new MyOtherRecord(Unsolved.thing());
    } catch (Exception e) {
    }
  }
}
