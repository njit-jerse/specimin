package com.example;

public class Simple {

  public static boolean hasDuplicates(byte[] a) {
    throw new java.lang.Error();
  }

  public static boolean hasNoDuplicates(byte[] a) {
    return !hasDuplicates(a);
  }
}
