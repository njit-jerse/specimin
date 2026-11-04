package com.example;

public class Simple {

  static String make() {
    throw new java.lang.Error();
  }

  void foo() {
    ObjectConstructor<String> c = Simple::make;
  }
}
