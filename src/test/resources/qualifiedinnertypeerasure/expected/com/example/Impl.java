package com.example;

import com.other.Missing;

public class Impl implements Rule {

  public String apply(Outer<String>.Inner<Integer> x, Missing y) {
    throw new java.lang.Error();
  }
}
