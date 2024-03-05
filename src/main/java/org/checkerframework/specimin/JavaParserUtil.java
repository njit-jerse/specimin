package org.checkerframework.specimin;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.comments.Comment;

/**
 * A class containing useful static functions using JavaParser.
 *
 * <p>This class cannot be instantiated.
 */
public class JavaParserUtil {

  /**
   * Private constructor to prevent instantiation.
   *
   * @throws UnsupportedOperationException if an attempt is made to instantiate this class.
   */
  private JavaParserUtil() {
    throw new UnsupportedOperationException("This class cannot be instantiated.");
  }

  /**
   * Removes a node from its compilation unit. If a node cannot be removed directly, it might be
   * wrapped inside another node, causing removal failure. This method iterates through the parent
   * nodes until it successfully removes the specified node.
   *
   * <p>If this explanation does not make sense to you, please refer to the following link for
   * further details: <a
   * href="https://github.com/javaparser/javaparser/issues/858">https://github.com/javaparser/javaparser/issues/858</a>
   *
   * @param node The node to be removed.
   */
  public static void removeNode(Node node) {
    while (!node.remove()) {
      node = node.getParentNode().get();
    }
  }

  /**
   * Get the signature of a method, not including the fully qualified class path. This method is a
   * simplified version of getDeclarationAsString from JavaParser.
   *
   * @param method The method declaration.
   * @return The method signature without the class path.
   */
  public static String extractMethodSignature(MethodDeclaration method) {
    StringBuilder sb = new StringBuilder();
    sb.append(method.getNameAsString());
    sb.append("(");
    boolean firstParam = true;
    for (Parameter param : method.getParameters()) {
      if (firstParam) {
        firstParam = false;
      } else {
        sb.append(", ");
      }
      sb.append(param.getType().toString());
      if (param.isVarArgs()) {
        sb.append("...");
      }
    }
    sb.append(")");
    return sb.toString();
  }

  /**
   * Given a compilation unit, this method returns a no-comment version of that compilation unit.
   *
   * @param cu a compilation unit
   * @return cu without any comments
   */
  public static CompilationUnit removeCommentsFromCompilationUnit(CompilationUnit cu) {
    CompilationUnit cuWithNoComments = cu;
    for (Comment child : cuWithNoComments.getAllComments()) {
      child.remove();
    }
    return cuWithNoComments;
  }
}
