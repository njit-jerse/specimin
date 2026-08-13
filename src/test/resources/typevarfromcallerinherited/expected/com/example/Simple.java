package com.example;

import org.testing.SimpleParent;

class Simple<E, V> extends SimpleParent<E, V> {

  public V get(E input) {
    V first = this.fetch(input);
    return store(first);
  }
}
