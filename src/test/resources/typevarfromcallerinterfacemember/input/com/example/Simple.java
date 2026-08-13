package com.example;

import com.example.other.Box;
import com.example.other.Src;

public interface Simple<C> {
  class Helper {
    public <T> Box<T> target(Src<T> src) {
      return Box.from(src);
    }
  }
}
