package com.example;

import com.example.other.Box;
import com.example.other.Holder;
import com.example.other.Src;

public class Simple {
  public static <T> Box<T> target(Src<T> src, Holder h) {
    return h.wrap(src);
  }
}
