package com.example;

import android.content.ComponentCallbacks2;

public class Simple implements ComponentCallbacks2, Listener {
  Holder bar() {
    return new Holder(this);
  }
}
