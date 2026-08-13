package com.example;

import com.example.other.Box;
import com.example.other.Src;

public class Simple<C> {

  public static <T> Box<T> target(Src<T> src) {
    return Box.from(src);
  }
}
