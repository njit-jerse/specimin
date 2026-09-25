package com.example;

import android.view.View;

class Box<T> {
  void put(View v) {}

  void put(String s) {}

  void put(T t) {}

  void use(T t) {
    put(t);
  }
}
