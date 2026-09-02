package com.example;

import org.other.Absent;

public class Simple {
  void bar() {
    new Container<Absent>().get().absentMethod();
  }
}
