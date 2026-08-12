package com.example;

import org.example.Handler;

public class Simple {
  public void target() {
    Handler h = Foo::mref;
    System.out.println(h);
  }
}
