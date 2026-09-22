package com.other;

public class Outer {

  public static class Inner {

    public static com.other.Outer.ComOtherOuterInnerUPSyntheticType UP;
  }

  public static class ComOtherOuterInnerUPSyntheticType {

    public com.other.Outer.FooReturnType foo() {
      throw new java.lang.Error();
    }
  }

  public static class FooReturnType {
  }
}
