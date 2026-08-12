package com.example;

import com.example.other.Wrapper;

public class Simple {
  public Object target(Wrapper w) {
    return w.map(
        item -> {
          if (item.flag()) {
            return item;
          } else {
            return item;
          }
        });
  }
}
