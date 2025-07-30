package org.checkerframework.specimin.unsolved;

import com.github.javaparser.ast.Node;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Base type for all synthetic definitions containing alternates. */
public abstract class UnsolvedSymbolAlternates<T extends UnsolvedSymbolAlternate> {
  private final List<UnsolvedClassOrInterfaceAlternates> alternateDeclaringTypes;
  private List<T> alternates = new ArrayList<>();

  protected UnsolvedSymbolAlternates(
      List<UnsolvedClassOrInterfaceAlternates> alternateDeclaringTypes) {
    this.alternateDeclaringTypes = alternateDeclaringTypes;
  }

  /**
   * Gets all possible fully qualified names of this unsolved symbol definition.
   *
   * @return All possible FQNS
   */
  public abstract Set<String> getFullyQualifiedNames();

  /**
   * Gets all possible declaring types for this symbol.
   *
   * @return All possible declaring types
   */
  public List<UnsolvedClassOrInterfaceAlternates> getAlternateDeclaringTypes() {
    return alternateDeclaringTypes;
  }

  /**
   * Gets alternate definitions for this symbol.
   *
   * @return All alternates
   */
  public List<T> getAlternates() {
    return alternates;
  }

  /**
   * Gets all the nodes that alternates could depend on.
   *
   * @return A set of all the nodes that alternates could depend on.
   */
  public Set<Node> getDependentNodes() {
    Set<Node> nodes = new HashSet<>();

    for (T alternate : alternates) {
      nodes.addAll(alternate.getMustPreserveNodes());
    }

    return nodes;
  }

  /**
   * Adds an alternate to this symbol's definition.
   *
   * @param alternate The alternate to add
   */
  protected void addAlternate(T alternate) {
    if (this.alternates == null) {
      this.alternates = new ArrayList<>();
    }
    this.alternates.add(alternate);
  }
}
