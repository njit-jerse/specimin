package com.example;

public class Simple {

  static void m() {
    throw new java.lang.Error();
  }

  void foo(boolean b) {
    Fn c = b ? Simple::m : null;
  }
}
