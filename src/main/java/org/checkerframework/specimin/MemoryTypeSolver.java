package org.checkerframework.specimin;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.model.SymbolReference;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import java.util.HashMap;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A simple in-memory type solver that allows us to store CompilationUnits and resolve types from
 * them.
 */
public class MemoryTypeSolver implements TypeSolver {

  private @Nullable TypeSolver parent;
  private Map<String, CompilationUnit> cache = new HashMap<>();

  @Override
  @SuppressWarnings(
      "nullness") // TypeSolver's getParent() is unannotated (and hence @NonNull) but docs specify
  // nullability
  public @Nullable TypeSolver getParent() {
    return parent;
  }

  @Override
  public void setParent(TypeSolver parent) {
    this.parent = parent;
  }

  @Override
  public SymbolReference<ResolvedReferenceTypeDeclaration> tryToSolveType(String name) {
    if (cache.containsKey(name)) {
      CompilationUnit cu = cache.get(name);
      return SymbolReference.solved(
          JavaParserFacade.get(this)
              .getTypeDeclaration(
                  cu.getTypes().stream()
                      .filter(
                          t ->
                              t.getFullyQualifiedName().isPresent()
                                  && t.getFullyQualifiedName().get().equals(name))
                      .findFirst()
                      .get()));
    }
    return SymbolReference.unsolved();
  }

  /**
   * Add a type to the in-memory type solver.
   *
   * @param name the fully qualified name of the type (e.g. "com.example.MyClass")
   * @param cu the CompilationUnit containing the type declaration
   */
  public void addType(String name, CompilationUnit cu) {
    cache.put(name, cu);
  }
}
