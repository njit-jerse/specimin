package com.example;

import org.other.Absent;
import org.other.Source;

public class Simple {
  void bar(Container<Absent> c, Source s) {
    c.set(s.produce());
  }
}
