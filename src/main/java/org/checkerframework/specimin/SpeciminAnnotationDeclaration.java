package org.checkerframework.specimin;

import com.github.javaparser.ast.AccessSpecifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.resolution.Context;
import com.github.javaparser.resolution.MethodUsage;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.resolution.declarations.ResolvedAnnotationMemberDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedParameterDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedTypeParameterDeclaration;
import com.github.javaparser.resolution.logic.MethodResolutionCapability;
import com.github.javaparser.resolution.model.SymbolReference;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.core.resolution.MethodUsageResolutionCapability;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserAnnotationDeclaration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A {@link JavaParserAnnotationDeclaration} that can resolve a call to one of its members, or to
 * one of the methods it inherits from {@code java.lang.annotation.Annotation}.
 *
 * <p>JavaParser models the members of an annotation type as {@link
 * ResolvedAnnotationMemberDeclaration}s, which are values rather than methods, so {@code
 * JavaParserAnnotationDeclaration} implements neither {@link MethodUsageResolutionCapability} nor
 * {@code getDeclaredMethods()} (<a
 * href="https://github.com/javaparser/javaparser/issues/1838">javaparser#1838</a>). Whenever
 * JavaParser needs the type of a call to an annotation member -- which it does for every argument
 * of a call, before it can pick an overload -- {@code ContextHelper#solveMethodAsUsage} therefore
 * throws a bare {@code UnsupportedOperationException}. Annotations read by reflection do not have
 * this problem, since {@code ReflectionAnnotationDeclaration} has the capability.
 *
 * <p>The same gap exists in the parallel {@link MethodResolutionCapability}, which {@code
 * MethodResolutionLogic#solveMethodInType} needs to resolve the call's declaration rather than its
 * type, so this class supplies both.
 *
 * <p>Instances of this class are substituted for {@code JavaParserAnnotationDeclaration}s by {@link
 * SpeciminCombinedTypeSolver}.
 */
public class SpeciminAnnotationDeclaration extends JavaParserAnnotationDeclaration
    implements MethodUsageResolutionCapability, MethodResolutionCapability {

  /**
   * Creates a declaration for the given annotation type.
   *
   * @param wrappedNode the annotation type's declaration in the AST
   * @param typeSolver the type solver to use
   */
  public SpeciminAnnotationDeclaration(AnnotationDeclaration wrappedNode, TypeSolver typeSolver) {
    super(wrappedNode, typeSolver);
  }

  @Override
  public Optional<MethodUsage> solveMethodAsUsage(
      String name,
      List<ResolvedType> argumentTypes,
      Context invokationContext,
      List<ResolvedType> typeParameters) {
    ResolvedMethodDeclaration member = findMember(name, argumentTypes, false);
    if (member != null) {
      return Optional.of(new MethodUsage(member));
    }
    for (ResolvedReferenceTypeDeclaration ancestor : ancestorDeclarations()) {
      // An ancestor without the capability is skipped rather than passed to ContextHelper, which
      // would throw the very exception this class exists to avoid.
      if (ancestor instanceof MethodUsageResolutionCapability capableAncestor) {
        Optional<MethodUsage> inherited =
            capableAncestor.solveMethodAsUsage(
                name, argumentTypes, invokationContext, typeParameters);
        if (inherited.isPresent()) {
          return inherited;
        }
      }
    }
    return Optional.empty();
  }

  @Override
  public SymbolReference<ResolvedMethodDeclaration> solveMethod(
      String name, List<ResolvedType> argumentsTypes, boolean staticOnly) {
    ResolvedMethodDeclaration member = findMember(name, argumentsTypes, staticOnly);
    if (member != null) {
      return SymbolReference.solved(member);
    }
    for (ResolvedReferenceTypeDeclaration ancestor : ancestorDeclarations()) {
      // As in solveMethodAsUsage: skip an ancestor that MethodResolutionLogic would throw on.
      if (ancestor instanceof MethodResolutionCapability capableAncestor) {
        SymbolReference<ResolvedMethodDeclaration> inherited =
            capableAncestor.solveMethod(name, argumentsTypes, staticOnly);
        if (inherited.isSolved()) {
          return inherited;
        }
      }
    }
    return SymbolReference.unsolved();
  }

  /**
   * Finds the member of this annotation type that a call would refer to.
   *
   * @param name the name of the called method
   * @param argumentTypes the types of the call's arguments
   * @param staticOnly whether the call can only refer to a static method
   * @return the matching member, presented as a method, or null if there is none
   */
  private @Nullable ResolvedMethodDeclaration findMember(
      String name, List<ResolvedType> argumentTypes, boolean staticOnly) {
    // A member of an annotation type takes no arguments and is never static (JLS 9.6.1), so a call
    // that does not fit that shape must be to something inherited instead.
    if (!argumentTypes.isEmpty() || staticOnly) {
      return null;
    }
    for (ResolvedAnnotationMemberDeclaration member : getAnnotationMembers()) {
      if (member.getName().equals(name)) {
        return new AnnotationMemberAsMethod(member, this);
      }
    }
    return null;
  }

  /**
   * Returns the declarations of this annotation type's ancestors. In practice the only one is
   * {@code java.lang.annotation.Annotation}, which is what a call to {@code annotationType()},
   * {@code toString()}, {@code hashCode()} or {@code equals()} on an annotation-typed expression
   * resolves to.
   *
   * @return the ancestors' declarations, omitting any ancestor that cannot be solved
   */
  private List<ResolvedReferenceTypeDeclaration> ancestorDeclarations() {
    List<ResolvedReferenceTypeDeclaration> ancestors = new ArrayList<>();
    for (ResolvedReferenceType ancestor : getAncestors(true)) {
      Optional<ResolvedReferenceTypeDeclaration> declaration = ancestor.getTypeDeclaration();
      if (declaration.isPresent()) {
        ancestors.add(declaration.get());
      }
    }
    return ancestors;
  }

  /**
   * Presents a member of an annotation type as the no-argument method that it is, so that it can be
   * put into a {@link MethodUsage}.
   */
  private static class AnnotationMemberAsMethod implements ResolvedMethodDeclaration {

    /** The member being presented as a method. */
    private final ResolvedAnnotationMemberDeclaration member;

    /** The annotation type that declares {@link #member}. */
    private final ResolvedReferenceTypeDeclaration declaringType;

    /**
     * Creates a method view of an annotation member.
     *
     * @param member the member
     * @param declaringType the annotation type that declares the member
     */
    AnnotationMemberAsMethod(
        ResolvedAnnotationMemberDeclaration member,
        ResolvedReferenceTypeDeclaration declaringType) {
      this.member = member;
      this.declaringType = declaringType;
    }

    @Override
    public String getName() {
      return member.getName();
    }

    @Override
    public ResolvedReferenceTypeDeclaration declaringType() {
      return declaringType;
    }

    @Override
    public ResolvedType getReturnType() {
      return member.getType();
    }

    @Override
    public int getNumberOfParams() {
      return 0;
    }

    @Override
    public ResolvedParameterDeclaration getParam(int i) {
      throw new IllegalArgumentException("An annotation member has no parameters: " + getName());
    }

    @Override
    public int getNumberOfSpecifiedExceptions() {
      return 0;
    }

    @Override
    public ResolvedType getSpecifiedException(int index) {
      throw new IllegalArgumentException("An annotation member throws nothing: " + getName());
    }

    @Override
    public List<ResolvedTypeParameterDeclaration> getTypeParameters() {
      return Collections.emptyList();
    }

    @Override
    public AccessSpecifier accessSpecifier() {
      // Implicitly public, per JLS 9.6.1.
      return AccessSpecifier.PUBLIC;
    }

    @Override
    public boolean isAbstract() {
      // Implicitly abstract, per JLS 9.6.1.
      return true;
    }

    @Override
    public boolean isDefaultMethod() {
      // "default" here refers to interface default methods, not an annotation member's
      // default value.
      return false;
    }

    @Override
    public boolean isStatic() {
      return false;
    }

    @Override
    public String toDescriptor() {
      return "()" + getReturnType().toDescriptor();
    }

    @Override
    public Optional<Node> toAst() {
      return member.toAst();
    }
  }
}
