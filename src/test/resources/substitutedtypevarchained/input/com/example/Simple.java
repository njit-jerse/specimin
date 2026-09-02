package com.example;

import org.other.Absent;

public class Simple {
  void bar(Outer<Absent> o) {
    o.inner().get().absentMethod();
  }
}
