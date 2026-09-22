package com.other;

public class Outer {

  public static class Inner {

    public static com.other.Outer.ComOtherOuterInnerFooReturnType foo() {
      throw new java.lang.Error();
    }
  }

  public static class ComOtherOuterInnerFooReturnType {

    public com.other.Outer.BazReturnType baz() {
      throw new java.lang.Error();
    }
  }

  public static class BazReturnType {
  }
}
