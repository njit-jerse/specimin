package com.example;

import com.example.other.Wrapper;

public class Simple {

  public void target(Wrapper w) {
    w.each(
        item -> {
          item.flag();
          return;
        });
  }
}
