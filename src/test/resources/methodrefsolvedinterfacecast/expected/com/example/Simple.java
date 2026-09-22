package com.example;

public class Simple {

  static void m() {
    throw new java.lang.Error();
  }

  void foo(boolean b) {
    Object c = (Fn) Simple::m;
  }
}
