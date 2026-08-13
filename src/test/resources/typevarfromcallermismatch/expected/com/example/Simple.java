package com.example;

import com.example.other.Src;

public class Simple {

  public static <T> void target(Src<String> src, T item) {
    src.put(item);
  }
}
