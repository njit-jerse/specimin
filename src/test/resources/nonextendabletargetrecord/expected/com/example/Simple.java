package com.example;

import org.example.Item;

class Simple {

  void bar(Item item) {
    Point pt = item.get();
    Payload p = item.get();
  }
}
