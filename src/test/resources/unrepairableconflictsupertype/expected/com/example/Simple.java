package com.example;

import org.example.Item;

class Simple {

  void bar(Item item) {
    Dog d = item.get();
    Animal a = item.get();
    System.out.println(d + "" + a);
  }
}
