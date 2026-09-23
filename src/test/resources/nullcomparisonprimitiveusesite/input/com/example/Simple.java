package com.example;

import org.example.Ctx;

class Simple {
  boolean bar(Ctx context) {
    int x = context.getParent();
    return context.getParent() == null;
  }
}
