package org.checkerframework.specimin.unsolved;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Base type for all synthetic definitions. */
public abstract class UnsolvedSymbolAlternates<T> {
  private List<T> alternates = new ArrayList<>();

  public abstract Set<String> getFullyQualifiedNames();

  public List<T> getAlternates() {
    return alternates;
  }

  public void addAlternate(T alternate) {
    if (this.alternates == null) {
      this.alternates = new ArrayList<>();
    }
    this.alternates.add(alternate);
  }
}
