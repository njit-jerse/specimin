package com.example;

import org.example.Foo;

public class Simple {
  public void target(Foo g) {
    Runnable mr = (Runnable) g::mref;
    System.out.println(mr);
  }
}
