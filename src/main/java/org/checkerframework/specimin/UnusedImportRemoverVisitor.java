package org.checkerframework.specimin;

import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;
import com.github.javaparser.resolution.declarations.ResolvedFieldDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Removes all unused import statements from a compilation unit. This visitor should be used after
 * pruning.
 */
public class UnusedImportRemoverVisitor extends ModifierVisitor<Void> {
  /**
   * A Map of fully qualified type/member names, or a wildcard import, to the actual import
   * declaration itself
   */
  private final Map<String, ImportDeclaration> typeNamesToImports = new HashMap<>();

  /** A set of all fully qualified type/member names in the current compilation unit */
  private final Set<String> usedImports = new HashSet<>();

  /** The package of the current compilation unit. */
  private String currentPackage = "";

  /**
   * Removes unused imports from the current compilation unit and resets the state to be used with
   * another compilation unit.
   */
  public void removeUnusedImports() {
    for (Map.Entry<String, ImportDeclaration> entry : typeNamesToImports.entrySet()) {
      if (!usedImports.contains(entry.getKey())) {
        entry.getValue().remove();
      } else if (entry.getKey().startsWith(currentPackage + ".")
          && !entry.getKey().substring(0, currentPackage.length() + 1).contains(".")) {
        // If importing a class from the same package, remove the unnecessary import
        entry.getValue().remove();
      }
    }

    typeNamesToImports.clear();
    usedImports.clear();
  }

  @Override
  public Node visit(ImportDeclaration decl, Void arg) {
    String importName = decl.getNameAsString();

    // ImportDeclaration does not contain the asterick by default; we need to add it
    if (decl.isAsterisk()) {
      importName += ".*";
    }

    typeNamesToImports.put(importName, decl);
    return super.visit(decl, arg);
  }

  @Override
  public Visitable visit(PackageDeclaration node, Void arg) {
    currentPackage = node.getNameAsString();

    return super.visit(node, arg);
  }

  @Override
  public Visitable visit(ClassOrInterfaceType type, Void arg) {
    String fullyQualified = JavaParserUtil.erase(type.resolve().describe());

    if (!fullyQualified.contains(".")) {
      // Type variable; definitely not imported
      return super.visit(type, arg);
    }

    String wildcard = fullyQualified.substring(0, fullyQualified.lastIndexOf('.')) + ".*";

    // Check for java.util.List and java.util.*
    usedImports.add(fullyQualified);
    usedImports.add(wildcard);
    return super.visit(type, arg);
  }

  @Override
  public Visitable visit(NameExpr expr, Void arg) {
    ResolvedValueDeclaration resolved = expr.resolve();
    // Handle statically imported fields
    // import static java.lang.Math.PI;
    // double x = PI;
    //            ^^
    if (resolved.isField()) {
      ResolvedFieldDeclaration asField = resolved.asField();
      String declaringType = JavaParserUtil.erase(asField.declaringType().getQualifiedName());
      // Check for both cases: java.lang.Math.PI and java.lang.Math.*
      usedImports.add(declaringType + "." + asField.getName());
      usedImports.add(declaringType + ".*");
    }
    return super.visit(expr, arg);
  }

  @Override
  public Visitable visit(MethodCallExpr expr, Void arg) {
    ResolvedMethodDeclaration resolved = expr.resolve();

    // Only static methods can be statically imported
    if (resolved.isStatic()) {
      // Handle statically imported methods
      // import static java.lang.Math.sqrt;
      // sqrt(1);
      // Check for both cases: java.lang.Math.sqrt and java.lang.Math.*
      usedImports.add(JavaParserUtil.erase(resolved.getQualifiedName()));
      usedImports.add(JavaParserUtil.erase(resolved.declaringType().getQualifiedName()) + ".*");
    }

    return super.visit(expr, arg);
  }
}
