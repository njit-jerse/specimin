package com.example;

import com.example.other.Wrapper;

public class Simple {
  public Object target(Wrapper w) {
    return w.map(
        item -> {
          Runnable r =
              () -> {
                item.flag();
                return;
              };
          r.run();
          return item;
        });
  }
}
