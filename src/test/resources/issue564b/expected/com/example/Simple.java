package com.example;

class Simple {

  Manager m;

  void foo(Target target) {
    m.clear(target);
  }
}

class Manager {

  public void clear(Target target) {
    throw new java.lang.Error();
  }
}

interface Target {}
