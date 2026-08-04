package com.example;

import org.example.Foo;

public class Simple {

  public void target(Foo f) {
    String s = (String) f.get();
    System.out.println(s);
  }
}
