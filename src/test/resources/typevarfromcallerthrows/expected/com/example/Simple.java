package com.example;

import com.example.other.Box;
import com.example.other.Src;
import java.io.IOException;

public class Simple {

  public static <V> Box<V> target(Src<V> src) {
    try {
      return Box.from(src);
    } catch (IOException e) {
      return null;
    }
  }
}
