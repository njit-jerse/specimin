package com.example;

import android.app.Fragment;

class Simple {

  Manager m;

  void foo(Sub s) {
    m.clear(s);
  }
}

class Sub extends Fragment implements Target {}

class Manager {

  public void clear(Target target) {
    throw new java.lang.Error();
  }
}

interface Target {}
