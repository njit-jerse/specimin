package com.example;

public class Simple {

  private static <T> ObjectConstructor<T> makeConstructor() {
    return () -> null;
  }
}
