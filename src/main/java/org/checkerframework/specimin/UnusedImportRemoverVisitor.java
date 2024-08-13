package org.checkerframework.specimin;

import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MarkerAnnotationExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;
import com.github.javaparser.resolution.UnsolvedSymbolException;
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
      } else if (!currentPackage.equals("")
          && entry.getKey().startsWith(currentPackage + ".")
          && !entry.getKey().substring(currentPackage.length() + 1).contains(".")) {
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
    // Workaround for a JavaParser bug: see UnsolvedSymbolVisitor#visit(ClassOrInterfaceType)
    // Also, if it's already fully qualified, it's not tied to an import
    if (!JavaParserUtil.isCapital(type.getName().asString())
        || JavaParserUtil.isAClassPath(type.getName().asString())) {
      return super.visit(type, arg);
    }

    String fullyQualified;
    try {
        fullyQualified = JavaParserUtil.erase(type.resolve().describe());
    } catch (UnsolvedSymbolException ex) {
        // Specimin made an error somewhere if this type is unresolvable;
        // TODO: fix this once MethodReturnFullyQualifiedGenericTest is fixed
        return super.visit(type, arg);
    }

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
  public Visitable visit(FieldAccessExpr expr, Void arg) {
    if (expr.hasScope()) {
      handleScopeExpression(expr.getScope());
    }
    return super.visit(expr, arg);
  }

  @Override
  public Visitable visit(NameExpr expr, Void arg) {
    if (expr.getParentNode().isPresent()
        && (expr.getParentNode().get() instanceof FieldAccessExpr
            || expr.getParentNode().get() instanceof MethodCallExpr)) {
      // If it's a field access/method call expression, other methods will handle this
      // If it's part of a fully qualified name, then we definitely do not need to handle this.
      return super.visit(expr, arg);
    }

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
    ResolvedMethodDeclaration resolved;
    try {
      resolved = expr.resolve();
    } catch (UnsupportedOperationException ex) {
      // Lambdas can raise an UnsupportedOperationException
      return super.visit(expr, arg);
    }

    if (resolved.isStatic()) {
      // If it has a scope, the parent class is imported
      if (expr.hasScope()) {
        handleScopeExpression(expr.getScope().get());
      } else {
        // Handle statically imported methods
        // import static java.lang.Math.sqrt;
        // sqrt(1);
        // Check for both cases: java.lang.Math.sqrt and java.lang.Math.*
        usedImports.add(JavaParserUtil.erase(resolved.getQualifiedName()));
        usedImports.add(JavaParserUtil.erase(resolved.declaringType().getQualifiedName()) + ".*");
      }
    }

    return super.visit(expr, arg);
  }

  @Override
  public Visitable visit(MarkerAnnotationExpr anno, Void arg) {
    handleAnnotation(anno);

    return super.visit(anno, arg);
  }

  @Override
  public Visitable visit(NormalAnnotationExpr anno, Void arg) {
    handleAnnotation(anno);

    return super.visit(anno, arg);
  }

  @Override
  public Visitable visit(SingleMemberAnnotationExpr anno, Void arg) {
    handleAnnotation(anno);

    return super.visit(anno, arg);
  }

  /**
   * Helper method to handle the scope type in a FieldAccessExpr or MethodCallExpr.
   *
   * @param scope The scope as an Expression
   */
  private void handleScopeExpression(Expression scope) {
    // Workaround for a JavaParser bug: see UnsolvedSymbolVisitor#visit(ClassOrInterfaceType)
    if (!JavaParserUtil.isCapital(scope.toString())) {
      return;
    }

    String fullyQualified = JavaParserUtil.erase(scope.calculateResolvedType().describe());

    if (!fullyQualified.contains(".")) {
      // If there is no ., it is not a class (i.e. this.values.length)
      return;
    }

    String wildcard = fullyQualified.substring(0, fullyQualified.lastIndexOf('.')) + ".*";

    usedImports.add(fullyQualified);
    usedImports.add(wildcard);
  }

  /**
   * Helper method to resolve all annotation expressions and add them to usedImports.
   *
   * @param anno The annotation expression to handle
   */
  private void handleAnnotation(AnnotationExpr anno) {
    String fullyQualified = JavaParserUtil.erase(anno.resolve().getQualifiedName());
    String wildcard = fullyQualified.substring(0, fullyQualified.lastIndexOf('.')) + ".*";

    // Check for java.lang.annotation.Target and java.lang.annotation.*
    usedImports.add(fullyQualified);
    usedImports.add(wildcard);
  }
}
