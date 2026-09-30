package com.example;

class Simple {

  Manager m;

  <Y extends Target> void foo(Y target) {
    m.clear(target);
  }
}

class Manager {

  public void clear(Target target) {
    throw new java.lang.Error();
  }
}

interface Target {}
