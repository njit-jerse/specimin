package com.example;

class Simple<T> {

  Manager m;

  <Y extends Target<T>> void foo(Y target) {
    m.clear(target);
  }
}

class Manager {

  public void clear(Target<?> target) {
    throw new java.lang.Error();
  }
}

interface Target<R> {}
