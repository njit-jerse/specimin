package com.example;

public class Simple {

  void foo(Anno anno) {
    sinkClass(anno.annotationType());
    sinkString(anno.toString());
  }

  void sinkClass(Class<?> clazz) {
    throw new java.lang.Error();
  }

  void sinkString(String value) {
    throw new java.lang.Error();
  }
}
