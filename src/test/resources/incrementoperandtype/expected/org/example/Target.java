package org.example;

import external.Metrics;

public class Target {

  public char charPrefix(Metrics metrics) {
    char result = ++metrics.code;
    return result;
  }

  public short shortPostfix(Metrics metrics) {
    short result = metrics.small++;
    return result;
  }

  public char charReturn(Metrics metrics) {
    return ++metrics.other;
  }

  public Object boxed(Metrics metrics) {
    Object result = ++metrics.opaque;
    return result;
  }
}
