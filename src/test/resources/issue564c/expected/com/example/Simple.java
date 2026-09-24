package com.example;

import android.view.View;

class Simple {

  Manager m;

  void foo(MyView target) {
    m.clear(target);
  }
}

class MyView extends View {}

class Manager {

  public void clear(View view) {
    throw new java.lang.Error();
  }
}
