package com.example;

import com.example.other.Wrapper;

public class Simple {

  public Object target(Wrapper w, Dog dog, Cat cat) {
    return w.map(
        item -> {
          if (item.flag()) {
            return dog;
          }
          return cat;
        });
  }
}
