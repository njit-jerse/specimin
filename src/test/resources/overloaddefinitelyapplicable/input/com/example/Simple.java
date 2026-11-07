package com.example;

import android.app.Fragment;
import android.view.View;

class Simple {
  Manager m;

  void foo(Sub s) {
    m.clear(s);
  }
}

class Sub extends Fragment implements Target {}

class Manager {
  public void clear(View view) {}

  public void clear(Target target) {}
}

interface Target {}
