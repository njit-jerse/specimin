package com.example;

import org.example.Item;

class Simple {

  void bar(Item item) {
    Color c = item.get();
    Payload p = item.get();
  }
}
