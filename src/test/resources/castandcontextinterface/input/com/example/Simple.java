package com.example;

import org.example.Bar;
import org.example.Foo;

public class Simple {
  public void target(Foo f) {
    Bar b = f.get();
    Runnable r = (Runnable) f.get();
    System.out.println("" + b + r);
  }
}
