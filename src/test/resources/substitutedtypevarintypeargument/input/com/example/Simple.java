package com.example;

import org.other.Absent;

public class Simple {
  void bar(Container<Absent> c) {
    c.all().get(0).absentMethod();
  }
}
