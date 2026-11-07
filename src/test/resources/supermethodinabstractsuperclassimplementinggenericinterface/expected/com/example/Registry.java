package com.example;

import org.other.Info;

public class Registry extends AbstractRegistry {

  public void register(Info info) {
    int leaseDuration = 90;
    super.register(info, leaseDuration);
  }
}
