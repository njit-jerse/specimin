package com.example;

import android.app.Fragment;

class Simple {

  Manager m;

  <Y extends Sub> void foo(Y target) {
    m.clear(target);
  }
}

class Sub extends Fragment implements Target {}

class Manager {

  public void clear(Target target) {
    throw new java.lang.Error();
  }
}

interface Target {}
