package org.checkerframework.specimin;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import java.util.HashMap;
import java.util.Map;
import org.checkerframework.checker.signature.qual.ClassGetSimpleName;

/**
 * This visitor is designed to assist the UnsolvedSymbolVisitor in creating synthetic files for
 * unsolved NameExpr instances by listing all the names of declared fields in the current input
 * file. It's important to note that this visitor is intended to be used in conjunction with the
 * UnsolvedSymbolVisitor, so both visitors will traverse the same Java file.
 * Thus, @ClassGetSimpleName names for involved classes should be sufficient.
 */
public class FieldDeclarationsVisitor extends VoidVisitorAdapter<Void> {
  /**
   * A mapping of field names to the @ClassGetSimpleName name of the classes in which they are
   * declared. Since inner classes can be involved, a map is used instead of a simple list.
   */
  Map<String, @ClassGetSimpleName String> fieldNameToClassNameMap;

  /** Constructs a new FieldDeclarationsVisitor. */
  public FieldDeclarationsVisitor() {
    fieldNameToClassNameMap = new HashMap<>();
  }

  @Override
  public void visit(FieldDeclaration decl, Void p) {
    // Caution: Calling .get() on an Optional without checking if it's present.
    // This will throw NoSuchElementException if the Optional returned by getParentNode()
    // is empty.
    ClassOrInterfaceDeclaration classNode =
        (ClassOrInterfaceDeclaration) decl.getParentNode().get();
    SimpleName classNodeSimpleName = classNode.getName();
    String className = classNodeSimpleName.asString();
    for (VariableDeclarator var : decl.getVariables()) {
      fieldNameToClassNameMap.put(var.getNameAsString(), className);
    }
  }

  /**
   * Get the value of fieldAndItsClass
   *
   * @return the value of fieldAndItsClass
   */
  public Map<String, @ClassGetSimpleName String> getFieldAndItsClass() {
    return fieldNameToClassNameMap;
  }
}
