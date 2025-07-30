package org.checkerframework.specimin.unsolved;

import java.util.List;

/**
 * Once an operation in {@link UnsolvedSymbolGenerator} is done, an instance of this record is
 * returned to represent what needs to be added to the slice and what needs to be removed.
 */
public record UnsolvedGenerationResult(
    List<UnsolvedSymbolAlternates<?>> toAdd, List<UnsolvedSymbolAlternates<?>> toRemove) {
  /** Represents a result where no symbols need to be added, and none need to be removed. */
  public static UnsolvedGenerationResult EMPTY = new UnsolvedGenerationResult(List.of(), List.of());
}
