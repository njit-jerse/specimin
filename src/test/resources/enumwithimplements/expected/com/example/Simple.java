package com.example;

public class Simple {

  private enum MyEnum implements Function<MyEnum, MyEnum> {
    A,
    B
  }

  void bar() {
    MyEnum a = MyEnum.A;
    MyEnum b = MyEnum.B;
  }
}
