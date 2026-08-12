package com.example;

import java.util.function.Function;

public class Simple {

  public void target() {
    Function<String, Foo> mr = Foo::new;
    System.out.println(mr);
  }
}
