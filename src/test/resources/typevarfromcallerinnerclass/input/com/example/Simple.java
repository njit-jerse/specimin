package com.example;

import com.example.other.Box;
import com.example.other.Src;

public class Simple<C> {
  class Inner {
    public Box<C> target(Src<C> src) {
      return Box.from(src);
    }
  }
}
