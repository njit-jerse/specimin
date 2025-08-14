package org.checkerframework.specimin.unsolved;

import com.github.javaparser.ast.Node;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;

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

  /** Removes duplicate alternates. */
  public void removeDuplicateAlternates() {
    Set<T> uniqueAlternates = new LinkedHashSet<>(alternates);
    alternates.clear();
    alternates.addAll(uniqueAlternates);
  }

  /**
   * Utility method to apply a transformation to all alternates. For example, if you want to set all
   * alternates to an interface, pass in {@code UnsolvedClassOrInterface::setIsAnInterfaceToTrue}.
   *
   * @param apply A Consumer that modifies each alternate. Pass in an instance method from {@link T}
   *     with no parameters.
   */
  public void applyToAllAlternates(Consumer<T> apply) {
    for (T alternate : alternates) {
      apply.accept(alternate);
    }
  }

  /**
   * Utility method to apply a transformation to all alternates. For example, if you want all
   * alternates to extend type "Foo", pass in {@code UnsolvedClassOrInterface::extend} and "Foo".
   *
   * @param apply A BiConsumer that modifies each alternate. Pass in an instance method from {@link
   *     T} with one parameter.
   * @param input The input to use to set all alternates.
   */
  public <U> void applyToAllAlternates(BiConsumer<T, U> apply, U input) {
    for (T alternate : alternates) {
      apply.accept(alternate, input);
    }
  }

  /**
   * Returns true if all alternates return true for a predicate. You can pass in a method reference
   * (like UnsolvedClassOrInterface::isAnInterface) to check if all alternates are an interface, for
   * example.
   *
   * @param predicate A predicate; pass in an instance method from {@link T} with no parameters.
   * @return True if all alternates return true for the predicate
   */
  public boolean doAllAlternatesReturnTrueFor(Predicate<T> predicate) {
    for (T alternate : alternates) {
      if (!predicate.test(alternate)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Returns true if all alternates return true for a predicate. You can pass in a method reference
   * (like UnsolvedClassOrInterface::doesImplement) and an interface "MyInterface" to check if all
   * alternates implement the "MyInterface" interface.
   *
   * @param predicate A BiPredicate; pass in an instance method from {@link T} with one parameter.
   * @return True if all alternates return true for the predicate
   */
  public <U> boolean doAllAlternatesReturnTrueFor(BiPredicate<T, U> predicate, U input) {
    for (T alternate : alternates) {
      if (!predicate.test(alternate, input)) {
        return false;
      }
    }
    return true;
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
    this.alternates.add(alternate);
  }
}
