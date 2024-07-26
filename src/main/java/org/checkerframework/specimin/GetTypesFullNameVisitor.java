package org.checkerframework.specimin;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A visitor that traverses a Java file's AST and creates a map, associating the file name with the
 * set of types used inside it.
 */
public class GetTypesFullNameVisitor extends ModifierVisitor<Void> {

  /** The directory path of the Java file. */
  private String fileDirectory = "";

  /**
   * A map that associates the file directory with the set of fully qualified names of types used
   * within that file.
   */
  private Map<String, Set<String>> fileAndAssociatedTypes = new HashMap<>();

  /**
   * Get the map of files' directories and types used within those files.
   *
   * @return the value of fileAndAssociatedTypes
   */
  public Map<String, Set<String>> getFileAndAssociatedTypes() {
    return Collections.unmodifiableMap(fileAndAssociatedTypes);
  }

  @Override
  public Visitable visit(ClassOrInterfaceDeclaration decl, Void p) {
    // Nested type and local classes don't have a separate class file.
    if (!decl.isNestedType() && !decl.isLocalClassDeclaration()) {
      // getFullyQualifiedName is always present for non-local classes.
      fileDirectory = decl.getFullyQualifiedName().orElseThrow().replace(".", "/") + ".java";
      fileAndAssociatedTypes.put(fileDirectory, new HashSet<>());
    }
    return super.visit(decl, p);
  }

  @Override
  public Visitable visit(EnumDeclaration decl, Void p) {
    // Nested types don't have a separate class file.
    if (!decl.isNestedType()) {
      // getFullyQualifiedName is always present for non-local classes, but enum declarations can't
      // be local before Java 16. We only currently technically only support up to Java 11
      // (due to JavaParser version restrictions). Until we upgrade JavaParser, this is safe.
      fileDirectory = decl.getFullyQualifiedName().orElseThrow().replace(".", "/") + ".java";
      fileAndAssociatedTypes.put(fileDirectory, new HashSet<>());
    }
    return super.visit(decl, p);
  }

  @Override
  public Visitable visit(ClassOrInterfaceType type, Void p) {
    String typeFullName;
    try {
      typeFullName =
          JavaParserUtil.classOrInterfaceTypeToResolvedReferenceType(type).getQualifiedName();
    } catch (UnsolvedSymbolException | UnsupportedOperationException | IllegalArgumentException e) {
      return super.visit(type, p);
    }
    if (fileAndAssociatedTypes.containsKey(fileDirectory)) {
      fileAndAssociatedTypes.get(fileDirectory).add(typeFullName);
      return super.visit(type, p);
    } else {
      throw new RuntimeException(
          "Unexpected files and types: " + fileDirectory + ", " + typeFullName);
    }
  }
}
