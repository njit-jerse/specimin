package org.checkerframework.specimin.unsolved;

import java.util.List;

/**
 * Once information adding is done in {@link UnsolvedSymbolGenerator}, an instance of this record is
 * returned to represent what needs to be added to the slice and what needs to be removed.
 */
public record AddInformationResult(
    List<UnsolvedSymbolAlternates<?>> toAdd, List<UnsolvedSymbolAlternates<?>> toRemove) {
  /** Represents a result where no symbols need to be added, and none need to be removed. */
  public static AddInformationResult EMPTY = new AddInformationResult(List.of(), List.of());
}
