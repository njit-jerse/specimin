package org.example;

import external.Metrics;

public class Target {

  public int twice(Metrics metrics) {
    return metrics.value() + metrics.value();
  }

  public boolean bothSides(Metrics metrics) {
    return metrics.flag & metrics.flag;
  }
}
