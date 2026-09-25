package com.example;

import android.app.Fragment;
import android.view.View;

class Simple {
  Manager m;

  <Y extends Sub> void foo(Y target) {
    m.clear(target);
  }
}

class Sub extends Fragment implements Target {}

class Manager {
  public void clear(View view) {}

  public void clear(Target target) {}
}

interface Target {}
