package org.checkerframework.specimin;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.model.SymbolReference;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserAnnotationDeclaration;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import java.util.IdentityHashMap;
import java.util.Optional;

/**
 * A {@link CombinedTypeSolver} that hands back {@link SpeciminAnnotationDeclaration}s in place of
 * the {@link JavaParserAnnotationDeclaration}s that JavaParser would otherwise produce for
 * annotation types declared in the input.
 *
 * <p>The substitution happens here, rather than at the solvers that create the declarations,
 * because a declaration also reaches callers straight out of this class' cache -- see {@link
 * SpeciminTypeSolvers#overrideCache}.
 */
public class SpeciminCombinedTypeSolver extends CombinedTypeSolver {

  /**
   * The substitute for each annotation type seen so far, keyed by AST node. Reusing a substitute
   * keeps {@code ResolvedReferenceType#equals}, which compares type declarations by identity,
   * behaving as it does for the cached declarations this class replaces.
   */
  private final IdentityHashMap<AnnotationDeclaration, SpeciminAnnotationDeclaration> substitutes =
      new IdentityHashMap<>();

  /**
   * Creates a combined type solver from the given solvers.
   *
   * @param elements the solvers to combine, in the order in which they should be consulted
   */
  public SpeciminCombinedTypeSolver(TypeSolver... elements) {
    super(elements);
  }

  @Override
  public SymbolReference<ResolvedReferenceTypeDeclaration> tryToSolveType(String name) {
    return substituteAnnotationDeclaration(super.tryToSolveType(name));
  }

  @Override
  public SymbolReference<ResolvedReferenceTypeDeclaration> tryToSolveTypeInModule(
      String packageQualifiedName, String simpleName) {
    return substituteAnnotationDeclaration(
        super.tryToSolveTypeInModule(packageQualifiedName, simpleName));
  }

  /**
   * Replaces a solved {@link JavaParserAnnotationDeclaration} with its {@link
   * SpeciminAnnotationDeclaration} substitute. Anything else is returned unchanged.
   *
   * @param reference the reference to a solved (or unsolved) type declaration
   * @return an equivalent reference whose declaration can solve calls to annotation members
   */
  private SymbolReference<ResolvedReferenceTypeDeclaration> substituteAnnotationDeclaration(
      SymbolReference<ResolvedReferenceTypeDeclaration> reference) {
    if (!reference.isSolved()) {
      return reference;
    }
    ResolvedReferenceTypeDeclaration declaration = reference.getCorrespondingDeclaration();
    if (!(declaration instanceof JavaParserAnnotationDeclaration)
        || declaration instanceof SpeciminAnnotationDeclaration) {
      return reference;
    }
    Optional<Node> ast = declaration.toAst();
    if (ast.isEmpty() || !(ast.get() instanceof AnnotationDeclaration annotationDeclaration)) {
      return reference;
    }
    return SymbolReference.solved(
        substitutes.computeIfAbsent(
            annotationDeclaration, node -> new SpeciminAnnotationDeclaration(node, this)));
  }
}
