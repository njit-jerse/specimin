package com.example;

import org.other.Factory;
import org.other.Info;

public class Outer {

  public Base foo(Factory factory, Info info) {
    return new Base() {

      public void run() {
        super.register(factory.make(), 90);
      }
    };
  }
}
