package com.example;

import org.example.Item;

class Simple {

  void bar(Item item) {
    Payload p = item.get();
    int x = item.get();
    System.out.println(p + "" + x);
  }
}
