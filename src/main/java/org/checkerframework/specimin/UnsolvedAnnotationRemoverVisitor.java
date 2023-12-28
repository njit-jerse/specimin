package org.checkerframework.specimin;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** A visitor that removes unsolved annotation expressions. */
public class UnsolvedAnnotationRemoverVisitor extends ModifierVisitor<Void> {
  /**
   * List of paths of jar files to be used as input. Note: this is the set of every jar path, not
   * just the jar paths used by the current compilation unit.
   */
  List<String> jarPaths;

  /** List of paths of jar files used by the annotations. */
  Set<String> usedJarPaths = new HashSet<>();

  /** Map every class in the set of jar files to the corresponding jar file */
  Map<String, String> classToJarPath = new HashMap<>();

  /**
   * Map a class to its fully qualified name based on the import statements of the current
   * compilation unit.
   */
  Map<String, String> classToFullClassName = new HashMap<>();

  /**
   * Create a new instance of UnsolvedAnnotationRemoverVisitor
   *
   * @param jarPaths a list of paths of jar files to be used as input
   */
  public UnsolvedAnnotationRemoverVisitor(List<String> jarPaths) {
    this.jarPaths = jarPaths;
    for (String jarPath : jarPaths) {
      try {
        JarTypeSolver jarSolver = new JarTypeSolver(jarPath);
        for (String fullClassName : jarSolver.getKnownClasses()) {
          classToJarPath.put(fullClassName, jarPath);
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  /**
   * Get the value of usedJarPaths
   *
   * @return the value of usedJarPaths
   */
  public Set<String> getUsedJarPaths() {
    return usedJarPaths;
  }

  @Override
  public Node visit(ImportDeclaration decl, Void p) {
    String classFullName = decl.getNameAsString();
    String className = classFullName.substring(classFullName.lastIndexOf(".") + 1);
    classToFullClassName.put(className, classFullName);
    return decl;
  }

  /**
   * Given a compilation unit, this method removes all unsolved annotations and record the related
   * jar paths of solved annotations.
   *
   * @param compilationUnit a compilation unit to be processed.
   */
  public void processAnnotations(CompilationUnit compilationUnit) {
    List<AnnotationExpr> annotationExprList = compilationUnit.findAll(AnnotationExpr.class);
    for (AnnotationExpr annotation : annotationExprList) {
      String annotationName = annotation.getNameAsString();
      if (!UnsolvedSymbolVisitor.isAClassPath(annotationName)) {
        // an annotation not imported is from the java.lang package, which is not our concern.
        if (!classToFullClassName.containsKey(annotationName)) {
          return;
        }
        annotationName = classToFullClassName.get(annotationName);
      }
      if (!classToJarPath.containsKey(annotationName)) {
        annotation.remove();
      } else {
        usedJarPaths.add(classToJarPath.get(annotationName));
      }
    }
  }
}
