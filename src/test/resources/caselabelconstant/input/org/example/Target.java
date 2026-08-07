package org.example;

public class Target {
  private static final String YES = "yes";

  public int target(String x) {
    switch (x) {
      case YES:
        return 1;
      default:
        return 0;
    }
  }
}
