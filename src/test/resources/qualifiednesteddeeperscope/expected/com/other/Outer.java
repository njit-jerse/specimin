package com.other;

public class Outer {

  public static class Inner {

    public static class Deeper {

      public static com.other.Outer.Inner.ComOtherOuterInnerDeeperUPSyntheticType UP;
    }

    public static class ComOtherOuterInnerDeeperUPSyntheticType {

      public com.other.Outer.Inner.FooReturnType foo() {
        throw new java.lang.Error();
      }
    }

    public static class FooReturnType {
    }
  }
}
