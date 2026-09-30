package com.example;

import org.other.Info;

public class Sub extends Base {
  public void foo(Info info) {
    super.register(info, 90);
  }
}
