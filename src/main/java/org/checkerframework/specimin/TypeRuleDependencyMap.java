package org.checkerframework.specimin;

import com.github.javaparser.ast.Node;
import java.util.List;

/**
 * This class provides a method to help determine what other elements are relevant when processing
 * an element.
 */
public interface TypeRuleDependencyMap {
  /**
   * Given a node, return all relevant nodes based on its type.
   *
   * @param node The node
   * @return All relevant nodes to the input node. For example, this could be annotations, type
   *     parameters, parameters, return type, etc. for methods.
   */
  public List<Node> getRelevantElements(Node node);

  public List<Node> getRelevantElements(Object resolved);
}
