package com.example;

import org.other.Absent;

public class Simple {

  void bar(Mid<Absent> m) {
    m.get().absentMethod();
  }
}
