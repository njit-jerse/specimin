package com.example;

import com.example.other.Holder;

public class Simple {

  public static <T> T target(Holder h) {
    return h.get();
  }
}
