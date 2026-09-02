package com.example;

import org.other.Absent;

public class Simple {
  <U> void bar(Container<Absent> c) {
    Runnable r =
        new Runnable() {
          Container<Absent> inner = c;

          U held = null;

          @Override
          public void run() {
            inner.get().absentMethod();
          }
        };
  }
}
