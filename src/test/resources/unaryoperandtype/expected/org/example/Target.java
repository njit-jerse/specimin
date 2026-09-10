package org.example;

import external.AotDetector;
import external.Counter;
import external.Flags;

public class Target {

  public String createDefaultApplicationContext() {
    if (!AotDetector.useGeneratedArtifacts()) {
      return "annotation-config";
    }
    return "generic";
  }

  public int mask(Flags flags) {
    return ~flags.bits();
  }

  public void tick(Counter counter) {
    counter.count++;
  }

  public double negate(Counter counter) {
    return -counter.scale();
  }
}
