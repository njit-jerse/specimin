package com.example;

import org.example.Item;

class Simple {

  void bar(Item item) {
    item.getPayload().foo();
    Payload p = item.getPayload();
  }
}
