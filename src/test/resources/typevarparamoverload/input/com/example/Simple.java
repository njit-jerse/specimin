package com.example;

import android.view.View;

class Simple {
  Manager m;

  <Y extends Target> void foo(Y target) {
    m.clear(target);
  }
}

class Manager {
  public <X extends View> void clear(X x) {}

  public void clear(Target target) {}
}

interface Target {}
