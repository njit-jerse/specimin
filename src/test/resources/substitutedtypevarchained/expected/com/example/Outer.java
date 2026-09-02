package com.example;

public class Outer<E> {

  public Inner<E> inner() {
    throw new java.lang.Error();
  }
}
