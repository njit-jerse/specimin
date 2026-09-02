package com.example;

import org.other.Absent;

public class Simple {
  void bar(Container<Container<Absent>> c) {
    c.get().get().absentMethod();
  }
}
