package com.example;

import com.example.other.Holder;
import com.example.other.Src;

public class Simple {
  public static <T> Holder target(Src<T> src) {
    return new Holder(src);
  }
}
