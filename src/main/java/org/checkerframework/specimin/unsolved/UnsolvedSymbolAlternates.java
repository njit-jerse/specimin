package org.checkerframework.specimin.unsolved;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Base type for all synthetic definitions. */
public abstract class UnsolvedSymbolAlternates<T> {
  private final List<UnsolvedClassOrInterfaceAlternates> alternateDeclaringTypes;
  private List<T> alternates = new ArrayList<>();

  protected UnsolvedSymbolAlternates(
      List<UnsolvedClassOrInterfaceAlternates> alternateDeclaringTypes) {
    this.alternateDeclaringTypes = alternateDeclaringTypes;
  }

  public abstract Set<String> getFullyQualifiedNames();

  public List<UnsolvedClassOrInterfaceAlternates> getAlternateDeclaringTypes() {
    return alternateDeclaringTypes;
  }

  public List<T> getAlternates() {
    return alternates;
  }

  protected void addAlternate(T alternate) {
    if (this.alternates == null) {
      this.alternates = new ArrayList<>();
    }
    this.alternates.add(alternate);
  }
}
