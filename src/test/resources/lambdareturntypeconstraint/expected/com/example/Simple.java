package com.example;

import com.example.other.Item;
import java.util.function.Supplier;

public class Simple {

  public String target(Item item) {
    Supplier<String> supplier =
        () -> {
          return item.getPayload();
        };
    return supplier.get();
  }
}
