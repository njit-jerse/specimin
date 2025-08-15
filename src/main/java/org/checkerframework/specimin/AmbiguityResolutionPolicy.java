package org.checkerframework.specimin;

import com.google.common.base.Ascii;

/** Represents the different ambiguity resolution policies. */
public enum AmbiguityResolutionPolicy {
  /** Generates all alternates */
  All,
  /** Generates the best effort alternates */
  BestEffort,
  /** Outputs the best set of alternates based on an input condition */
  InputCondition;

  /**
   * Gets the corresponding ambiguity resolution policy based on the input. Throws if invalid.
   *
   * @param input The input; accepts all, best-effort, and input-condition.
   * @return The enum value, if valid, or else an exception
   */
  public static AmbiguityResolutionPolicy parse(String input) {
    return switch (Ascii.toLowerCase(input)) {
      case "all" -> AmbiguityResolutionPolicy.All;
      case "best-effort" -> AmbiguityResolutionPolicy.BestEffort;
      case "input-condition" -> AmbiguityResolutionPolicy.InputCondition;
      default ->
          throw new RuntimeException(
              "Unsupported ambiguity resolution policy. Options are: \"all\", \"best-effort\","
                  + " \"input-condition\"");
    };
  }
}
