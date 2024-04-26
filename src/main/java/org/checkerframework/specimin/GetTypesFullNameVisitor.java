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
      /*
       * In JavaParser, ClassOrInterfaceType is a subtype of ReferenceType
       * (check an example here: https://github.com/javaparser/javaparser/blob/9c133d19d5b85b3b758f05762fb4d7c9875ef681/javaparser-core/src/main/java/com/github/javaparser/ast/type/ClassOrInterfaceType.java#L258).
       * However, the resolve() method in ClassOrInterfaceType only returns a ResolvedType instead of a specific ResolvedReferenceType.
       * This appears to be an inaccuracy within JavaParser's type hierarchy.
       */

      // since type is a ClassOrInterfaceType instance, it is safe to cast it to a ReferenceType.
      typeFullName = type.resolve().asReferenceType().getQualifiedName();
    } catch (UnsolvedSymbolException | UnsupportedOperationException e) {
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
