package com.example;

public class Foo implements MyComparable<String> {
  public int compareTo(String o) {
      throw new java.lang.Error();
  }

  public void foo() {
    throw new java.lang.Error();
  }
}
