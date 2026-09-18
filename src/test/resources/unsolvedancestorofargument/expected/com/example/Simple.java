package com.example;

import android.content.ComponentCallbacks2;

public class Simple implements ComponentCallbacks2, Listener {
  void bar(Lifecycle lifecycle) {
    lifecycle.addListener(this);
  }
}
