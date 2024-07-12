package org.checkerframework.specimin;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import org.checkerframework.checker.signature.qual.FullyQualifiedName;

import java.util.Optional;

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
   * Utility method to check if the given declaration is a local class declaration.
   *
   * @param decl a class, interface, or enum declaration
   * @return true iff the given declaration is of a local class
   */
  public static boolean isLocalClassDecl(TypeDeclaration<?> decl) {
    return decl.isClassOrInterfaceDeclaration()
        && decl.asClassOrInterfaceDeclaration().isLocalClassDeclaration();
  }

  /**
   * Given a ClassOrInterfaceType instance, this method returns a corresponding
   * ResolvedReferenceType. This method might throw an exception if the given instance is
   * unresolved.
   *
   * <p>Note: In JavaParser, ClassOrInterfaceType is a subtype of ReferenceType (check an example
   * here:
   * https://github.com/javaparser/javaparser/blob/9c133d19d5b85b3b758f05762fb4d7c9875ef681/javaparser-core/src/main/java/com/github/javaparser/ast/type/ClassOrInterfaceType.java#L258).
   * However, the resolve() method in ClassOrInterfaceType only returns a ResolvedType instead of a
   * specific ResolvedReferenceType. This appears to be an inaccuracy within JavaParser's type
   * hierarchy.
   *
   * @param type the ClassOrInterfaceType instance
   * @return the corresponding ResolvedReferenceType instance
   */
  public static ResolvedReferenceType classOrInterfaceTypeToResolvedReferenceType(
      ClassOrInterfaceType type) {
    return type.resolve().asReferenceType();
  }

    /**
     * Searches the ancestors of the given node until it finds a class or interface node, and then
     * returns the fully-qualified name of that class or interface.
     *
     * <p>This method will fail if it is called on a node that is not contained in a class or
     * interface.
     *
     * @param node a node contained in a class or interface
     * @return the fully-qualified name of the inner-most containing class or interface
     */
    @SuppressWarnings("signature") // result is a fully-qualified name or else this throws
    public static @FullyQualifiedName String getEnclosingClassName(Node node) {
      Node parent = getEnclosingClassLike(node);

      if (parent instanceof ClassOrInterfaceDeclaration) {
        return ((ClassOrInterfaceDeclaration) parent).getFullyQualifiedName().orElseThrow();
      }

      if (parent instanceof EnumDeclaration) {
        return ((EnumDeclaration) parent).getFullyQualifiedName().orElseThrow();
      }

      if (parent instanceof AnnotationDeclaration) {
        return ((AnnotationDeclaration) parent).getFullyQualifiedName().orElseThrow();
      }

      throw new RuntimeException("unexpected kind of node: " + parent.getClass());
    }

    /**
     * Returns the innermost enclosing class-like element for the given node. A class-like element is
     * a class, interface, or enum (i.e., something that would be a {@link
     * javax.lang.model.element.TypeElement} in javac's internal model). This method will throw if no
     * such element exists.
     *
     * @param node a node that is contained in a class-like structure
     * @return the nearest enclosing class-like node
     */
    public static Node getEnclosingClassLike(Node node) {
      Node parent = node.getParentNode().orElseThrow();
      while (!(parent instanceof ClassOrInterfaceDeclaration
              || parent instanceof EnumDeclaration
              || parent instanceof AnnotationDeclaration)) {
        parent = parent.getParentNode().orElseThrow();
      }
      return parent;
    }

    /**
     * Returns true iff the innermost enclosing class/interface is an enum.
     *
     * @param node any node
     * @return true if the enclosing class is an enum, false otherwise
     */
    public static boolean isInEnum(Node node) {
      Optional<Node> parent = node.getParentNode();
      while (parent.isPresent()) {
        Node actualParent = parent.get();
        if (actualParent instanceof EnumDeclaration) {
          return true;
        }
        parent = actualParent.getParentNode();
      }
      return false;
    }
}
