package com.example;

import org.other.Info;

public abstract class AbstractRegistry implements LeaseManager<Info> {
  public void register(Info registrant, int leaseDuration) {
    registrant.doSomething();
  }
}
