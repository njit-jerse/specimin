package com.example;

public class Simple {
  public void target(Foo g) {
    Runnable mr = (Runnable) g::mref;
    System.out.println(mr);
  }
}
