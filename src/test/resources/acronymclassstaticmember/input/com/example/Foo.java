package com.example;

public class Foo {
  public Object bar() {
    Object first = org.apache.commons.io.IOUtils.toString("x");
    Object second = org.apache.commons.utils.make("y");
    return first == null ? second : first;
  }
}
