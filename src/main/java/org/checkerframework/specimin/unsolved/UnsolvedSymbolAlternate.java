package org.checkerframework.specimin.unsolved;

import com.github.javaparser.ast.Node;
import java.util.Set;

/**
 * Base class for any SINGLE unsolved symbol alternate, such as UnsolvedClassOrInterface,
 * UnsolvedMethod, or UnsolvedField. Contains a getter for setting nodes that must be preserved,
 * given an alternate's definition.
 *
 * <p>{@link #getMustPreserveNodes()} is useful for certain ambiguities. For example:
 *
 * <pre><code>
 * import org.example.Foo;
 * class Simple {
 *    void bar() {
 *        Simple simple = new Simple(Foo.unsolvedMethod());
 *    }
 *
 *    private Simple(String string) { }
 *    private Simple(int x) { }
 * }
 * </code></pre>
 *
 * {@code Foo.unsolvedMethod()} could return either String or int, so two alternates will be
 * generated. Each alternate, based on its return type, will also preserve a different constructor,
 * which is why this base class is necessary.
 */
public abstract class UnsolvedSymbolAlternate {
  /**
   * Super constructor for all inheriting classes to set mustPreserve nodes.
   *
   * @param mustPreserve The set of nodes that must be preserved for this alternate.
   */
  public UnsolvedSymbolAlternate(Set<Node> mustPreserve) {
    this.mustPreserve = mustPreserve;
  }

  private final Set<Node> mustPreserve;

  /**
   * Gets the nodes that must be preserved for this alternate.
   *
   * @return The nodes that must be preserved for this alternate
   */
  public Set<Node> getMustPreserveNodes() {
    return mustPreserve;
  }
}
