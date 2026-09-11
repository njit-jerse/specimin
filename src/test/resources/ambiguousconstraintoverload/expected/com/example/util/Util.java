package com.example.util;

public class Util {

  public static <F, G, T> Iterable<T> transform2(
      Iterable<? extends F> a,
      Iterable<? extends G> b,
      java.util.function.BiFunction<? super F, ? super G, ? extends T> function) {
    throw new java.lang.Error();
  }
}
