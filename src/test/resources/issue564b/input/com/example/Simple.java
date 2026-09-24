package com.example;

import android.view.View;

class Simple {
  Manager m;

  void foo(Target target) {
    m.clear(target);
  }
}

class Manager {
  public void clear(View view) {}

  public void clear(Target target) {}
}

interface Target {}
