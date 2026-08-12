package com.example;

import org.example.Foo;

public class Simple {
  public void target(Foo g) {
    Runnable mr = g::mref;
    System.out.println(mr);
  }
}
