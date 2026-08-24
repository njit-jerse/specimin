package com.example;

import org.example.Item;

class Simple {

  void bar(Item item) {
    item.getPayload().foo();
    Object o = item.getPayload();
  }
}
