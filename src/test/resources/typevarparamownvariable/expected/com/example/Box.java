package com.example;

class Box<T> {

  void put(T t) {
    throw new java.lang.Error();
  }

  void use(T t) {
    put(t);
  }
}
