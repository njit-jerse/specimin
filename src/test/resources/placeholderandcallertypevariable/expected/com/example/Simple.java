package com.example;

import org.example.Item;

class Simple {

  <U> void bar(Item item, U arg) {
    U u = item.get(arg);
    Payload p = item.get(arg);
  }
}
