package com.example;

import org.other.Absent;

public class Simple {
  void bar(Container<Absent> c) {
    c.all()[0].absentMethod();
  }
}
