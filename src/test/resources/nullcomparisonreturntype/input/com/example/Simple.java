package com.example;

import org.example.Ctx;

class Simple {
  boolean bar(Ctx context) {
    return context.getParent() == null;
  }
}
