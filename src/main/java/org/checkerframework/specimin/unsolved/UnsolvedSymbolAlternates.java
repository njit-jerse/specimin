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
  /** A list of potential declaring types for this symbol. */
  private final List<UnsolvedClassOrInterfaceAlternates> alternateDeclaringTypes;

  /** A list of alternate definitions for this symbol. */
  private List<T> alternates = new ArrayList<>();

  /**
   * Base constructor for setting alternate declaring types.
   *
   * @param alternateDeclaringTypes The set of potential declaring types
   */
  protected UnsolvedSymbolAlternates(
      List<UnsolvedClassOrInterfaceAlternates> alternateDeclaringTypes) {
    this.alternateDeclaringTypes = new ArrayList<>(alternateDeclaringTypes);
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
  protected void applyToAllAlternates(Consumer<T> apply) {
    for (T alternate : alternates) {
      apply.accept(alternate);
    }
  }

  /**
   * Utility method to apply a transformation to all alternates. For example, if you want all
   * alternates to extend type "Foo", pass in {@code UnsolvedClassOrInterface::extend} and "Foo".
   *
   * @param <U> The type of the input parameter to the BiConsumer
   * @param apply A BiConsumer that modifies each alternate. Pass in an instance method from {@link
   *     T} with one parameter.
   * @param input The input to use to set all alternates.
   */
  protected <U> void applyToAllAlternates(BiConsumer<T, U> apply, U input) {
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
  protected boolean doAllAlternatesReturnTrueFor(Predicate<T> predicate) {
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
   * @param <U> The type of the input parameter to the BiPredicate
   * @param predicate A BiPredicate; pass in an instance method from {@link T} with one parameter.
   * @param input The input to use for the predicate
   * @return True if all alternates return true for the predicate
   */
  protected <U> boolean doAllAlternatesReturnTrueFor(BiPredicate<T, U> predicate, U input) {
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
