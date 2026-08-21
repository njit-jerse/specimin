package com.example;

import org.example.Item;

class Simple {

  void bar(Item item) {
    Payload p = item.getPayload();
    item.getPayload().foo();
  }
}
