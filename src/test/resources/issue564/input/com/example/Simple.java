package com.example;

import android.view.View;

class Simple<T> {
  Manager m;

  <Y extends Target<T>> void foo(Y target) {
    m.clear(target);
  }
}

class Manager {
  public void clear(View view) {}

  public void clear(Target<?> target) {}
}

interface Target<R> {}
