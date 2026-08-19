package com.example;

import org.example.Item;

class Simple {

  <U> void bar(Item item) {
    U u = item.get();
    Payload p = item.get();
  }
}
