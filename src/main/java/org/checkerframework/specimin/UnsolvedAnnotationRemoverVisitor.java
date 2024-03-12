package org.checkerframework.specimin;

import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MarkerAnnotationExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver;
import java.io.IOException;
import java.util.Arrays;
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

  /** Map every class in the set of jar files to the corresponding jar file */
  Map<String, String> classToJarPath = new HashMap<>();

  /**
   * Map a class to its fully qualified name based on the import statements of the current
   * compilation unit.
   */
  Map<String, String> classToFullClassName = new HashMap<>();

  /** The set of annotations predefined by java.lang. */
  static final Set<String> javaLangPredefinedAnnotations =
      new HashSet<>(Arrays.asList("Override", "Deprecated", "SuppressWarnings"));

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

  @Override
  public Node visit(ImportDeclaration decl, Void p) {
    String classFullName = decl.getNameAsString();
    String className = classFullName.substring(classFullName.lastIndexOf(".") + 1);
    classToFullClassName.put(className, classFullName);
    return decl;
  }

  @Override
  public Visitable visit(MarkerAnnotationExpr expr, Void p) {
    processAnnotations(expr);
    return super.visit(expr, p);
  }

  @Override
  public Visitable visit(NormalAnnotationExpr expr, Void p) {
    processAnnotations(expr);
    return super.visit(expr, p);
  }

  @Override
  public Visitable visit(SingleMemberAnnotationExpr expr, Void p) {
    processAnnotations(expr);
    return super.visit(expr, p);
  }

  /**
   * Processes annotations by removing annotations that are not solvable by the input list of jar
   * files.
   *
   * @param annotation the annotation to be processed
   */
  public void processAnnotations(AnnotationExpr annotation) {
    String annotationName = annotation.getNameAsString();
    if (!UnsolvedSymbolVisitor.isAClassPath(annotationName)) {
      // An annotation not imported is from the java.lang package or the same package as the input
      // file, which is not our concern.
      if (!classToFullClassName.containsKey(annotationName)) {
        if (!javaLangPredefinedAnnotations.contains(annotationName)) {
          annotation.remove();
        }
        return;
      }
      annotationName = classToFullClassName.get(annotationName);
    }
    if (!classToJarPath.containsKey(annotationName)) {
      annotation.remove();
    }
  }
}
