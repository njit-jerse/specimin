package com.example;

public class Outer {

  public static class Inner {

    public static com.example.Outer.ComExampleOuterInnerUPSyntheticType UP;
  }

  public static class ComExampleOuterInnerUPSyntheticType {

    public com.example.Outer.FooReturnType foo() {
      throw new java.lang.Error();
    }
  }

  public static class FooReturnType {
  }
}
