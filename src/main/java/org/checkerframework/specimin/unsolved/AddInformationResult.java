package org.checkerframework.specimin.unsolved;

import java.util.List;

/**
 * Once information adding is done in {@link UnsolvedSymbolGenerator}, an instance of this record is
 * returned to represent what needs to be added to the slice and what needs to be removed.
 */
public record AddInformationResult(
    List<UnsolvedSymbolAlternates<?>> toAdd, List<UnsolvedSymbolAlternates<?>> toRemove) {
  public AddInformationResult() {
    this(List.of(), List.of());
  }
}
