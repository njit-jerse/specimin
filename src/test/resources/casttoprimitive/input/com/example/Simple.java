package com.example;

import org.example.Foo;

public class Simple {
  public void target(Foo f) {
    int i = (int) f.get();
    System.out.println(i);
  }
}
