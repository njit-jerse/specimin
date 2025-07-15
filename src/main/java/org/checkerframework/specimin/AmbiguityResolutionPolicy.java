package org.checkerframework.specimin;

import com.google.common.base.Ascii;

public enum AmbiguityResolutionPolicy {
  All,
  BestEffort,
  InputCondition;

  public static AmbiguityResolutionPolicy parse(String input) {
    return switch (Ascii.toLowerCase(input)) {
      case "all" -> AmbiguityResolutionPolicy.All;
      case "best-effort" -> AmbiguityResolutionPolicy.BestEffort;
      case "input-condition" -> AmbiguityResolutionPolicy.InputCondition;
      default -> throw new IllegalArgumentException();
    };
  }
}
