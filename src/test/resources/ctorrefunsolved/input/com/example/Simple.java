package com.example;

import org.example.Foo;

public class Simple {
  public void target(Foo g) {
    Runnable mr = Foo::new;
    System.out.println(mr);
  }
}
