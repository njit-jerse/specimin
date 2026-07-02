package com.example;

public class Simple {

  private Anno anno;

  int foo() {
    return anno != null ? anno.foo() : 1;
  }
}
