package org.checkerframework.specimin;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithArguments;
import com.github.javaparser.resolution.MethodAmbiguityException;
import com.github.javaparser.resolution.Resolvable;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedAnnotationDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedAnnotationMemberDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Contains wrappers for {@link Resolvable#resolve()} and {@link Expression#calculateResolvedType()}
 * that handle known JavaParser bugs and return the correct result when possible.
 */
// This class must use Resolvable#resolve() and Expression#calculateResolvedType() because it is the
// wrapper.
@SuppressWarnings({"NoJavaParserResolve", "NoJavaParserCalculateResolvedType"})
public class Resolver {
  /**
   * Private constructor to prevent instantiation.
   *
   * @throws UnsupportedOperationException if an attempt is made to instantiate this class.
   */
  private Resolver() {
    throw new UnsupportedOperationException("This class cannot be instantiated.");
  }

  /**
   * A map of fully qualified names to compilation units. Must be set before any resolution is
   * attempted.
   */
  private static @MonotonicNonNull Map<String, CompilationUnit> fqnToCompilationUnits;

  /**
   * Set the map of fully qualified names to compilation units for use in our custom resolution
   * logic.
   *
   * @param fqnToCompilationUnits The map of fully qualified names to compilation units
   */
  @EnsuresNonNull("Resolver.fqnToCompilationUnits")
  public static void setFqnToCompilationUnitMap(
      Map<String, CompilationUnit> fqnToCompilationUnits) {
    Resolver.fqnToCompilationUnits = fqnToCompilationUnits;
  }

  /**
   * Equivalent to {@code expr.calculateResolvedType()}, but returns null if the type cannot be
   * resolved. Use instead of {@code expr.calculateResolvedType()} and try/catch {@link
   * UnsolvedSymbolException} since this handles JavaParser's other exceptions too.
   *
   * @param expr The expression
   * @return The resolved type of the expression, or null if it cannot be resolved
   */
  public static @Nullable ResolvedType calculateResolvedType(Expression expr) {
    try {
      return expr.calculateResolvedType();
    } catch (UnsolvedSymbolException | IllegalStateException ex) {
      // We can get:
      // * IllegalStateException when trying to resolve a lambda parameter that has the type of an
      // unbounded wildcard
      // * RuntimeException in certain cases with a block statement (unclear exactly why this
      // happens; all it matters is that it's an internal JavaParser bug)
      return null;
    } catch (RuntimeException ex) {
      // Put separately here because the exceptions above are all types of RuntimeExceptions
      return null;
    }
  }

  /**
   * Resolves a resolvable node. Use instead of {@link Resolvable#resolve()} because this handles
   * all known exceptions, and returns null when otherwise unresolvable.
   *
   * @param toResolve The node to resolve
   * @return The resolved object, or null if not resolvable
   * @param <T> The type to resolve to
   */
  @SuppressWarnings("unchecked")
  // All casts to T are ok. It's not possible for toResolve to suddenly resolve to a different type.
  public static <T> @Nullable T resolve(Resolvable<T> toResolve) {
    if (fqnToCompilationUnits == null) {
      throw new UnsupportedOperationException(
          "fqnToCompilationUnits must be set before calling resolve");
    }

    try {
      return toResolve.resolve();
    } catch (UnsolvedSymbolException ex) {
      return (T) tryAlternativeResolutionForUnsolvableNode((Node) toResolve);
    } catch (IllegalStateException ex) {
      return (T) Resolver.handleIllegalStateException(ex, (Node) toResolve);
    } catch (MethodAmbiguityException ex) {
      return (T) Resolver.handleMethodAmbiguityException(ex, (Node) toResolve);
    } catch (UnsupportedOperationException ex) {
      return (T) Resolver.handleUnsupportedOperationException(ex, (Node) toResolve);
    }
  }

  /**
   * Resolves a resolvable node. Use instead of {@link Resolvable#resolve()} because this handles
   * all known exceptions. Use when you are sure that the node is resolvable, and want to throw an
   * exception if it is not. Typically used with declarations, since those are almost always
   * solvable.
   *
   * @param toResolve The node to resolve
   * @return The resolved object
   * @param <T> The type to resolve to
   */
  @SuppressWarnings("unchecked")
  // All casts to T are ok. It's not possible for toResolve to suddenly resolve to a different type.
  public static <T> @NonNull T resolveGuaranteeNonNull(Resolvable<T> toResolve) {
    T result;

    try {
      result = toResolve.resolve();
    } catch (IllegalStateException ex) {
      Object resolved = Resolver.handleIllegalStateException(ex, (Node) toResolve);

      if (resolved == null) {
        throw ex;
      }

      result = (T) resolved;
    } catch (MethodAmbiguityException ex) {
      Object resolved = Resolver.handleMethodAmbiguityException(ex, (Node) toResolve);

      if (resolved == null) {
        throw ex;
      }

      result = (T) resolved;
    } catch (UnsupportedOperationException ex) {
      Object resolved = Resolver.handleUnsupportedOperationException(ex, (Node) toResolve);

      if (resolved == null) {
        throw ex;
      }

      result = (T) resolved;
    }

    if (result == null) {
      // This should never happen, but it's here to satisfy the null checker
      throw new RuntimeException("Resolved result was null");
    }

    return result;
  }

  /**
   * Tries alternative resolution strategies for a node that cannot be resolved through JavaParser's
   * symbol solver.
   *
   * @param unsolvable The unsolvable node
   * @return The resolved version of the node, or null if not resolvable.
   */
  private static @Nullable Object tryAlternativeResolutionForUnsolvableNode(Node unsolvable) {
    if (fqnToCompilationUnits == null) {
      throw new UnsupportedOperationException(
          "fqnToCompilationUnits must be set before calling"
              + " tryAlternativeResolutionForUnsolvableNode");
    }

    if (unsolvable instanceof Expression expr) {
      // Workaround for resolving methods/fields with a qualifier that is resolvable, but returns
      // a lambda constraint type with a type parameter instead of a type
      Object result =
          JavaParserUtil.tryFindCorrespondingDeclarationForConstraintQualifiedExpression(expr);

      if (result != null) {
        return result;
      }

      result = JavaParserUtil.tryResolveNodeIfInAnonymousClass(expr);

      if (result != null) {
        return result;
      }
    }

    // Handle cases where a method/constructor call cannot be resolved because of unresolvable
    // arguments, but its definition exists
    CallableDeclaration<?> potentiallyResolvableCallable =
        unsolvable instanceof NodeWithArguments<?> withArgs
            ? JavaParserUtil.tryFindSingleCallableForNodeWithUnresolvableArguments(
                withArgs, fqnToCompilationUnits)
            : null;
    if (potentiallyResolvableCallable != null) {
      return ((Resolvable<?>) potentiallyResolvableCallable).resolve();
    }
    return null;
  }

  /**
   * Handles an IllegalStateException thrown by JavaParser when resolving an expression whose scope
   * is a lambda parameter whose type is an unbounded wildcard.
   *
   * @param ex The exception
   * @param node The node
   * @return The resolved declaration or null if not found
   * @throws IllegalStateException if the node is not an expression
   */
  private static @Nullable Object handleIllegalStateException(IllegalStateException ex, Node node)
      throws IllegalStateException {
    if (!(node instanceof Expression)) {
      throw ex;
    }

    // IllegalStateExceptions are otherwise equivalent to UnsolvedSymbolExceptions, so we can try
    // the same alternative resolution strategies
    return tryAlternativeResolutionForUnsolvableNode(node);
  }

  /**
   * Handles a MethodAmbiguityException thrown by JavaParser when resolving a method call with known
   * argument types but there are multiple overloads. May return null if the method is resolvable
   * but is some method in the JDK.
   *
   * @param ex The exception
   * @param node The node
   * @return The method if found, or null if the method represents some method in the JDK
   * @throws MethodAmbiguityException when the MethodAmbiguityException comes from an issue Specimin
   *     does not know how to address
   */
  private static @Nullable ResolvedMethodDeclaration handleMethodAmbiguityException(
      MethodAmbiguityException ex, Node node) throws MethodAmbiguityException {
    if (fqnToCompilationUnits == null) {
      throw new UnsupportedOperationException(
          "fqnToCompilationUnits must be set before calling handleMethodAmbiguityException");
    }
    if (!ex.toString().contains("ReflectionMethodDeclaration")) {
      if (node instanceof MethodCallExpr methodCallExpr) {
        CallableDeclaration<?> potentialMethod =
            JavaParserUtil.tryFindSingleCallableForNodeWithUnresolvableArguments(
                methodCallExpr, fqnToCompilationUnits);

        if (potentialMethod != null) {
          return potentialMethod.asMethodDeclaration().resolve();
        }
      }

      throw ex;
    }

    return null;
  }

  /**
   * Handles an UnsupportedOperationException thrown by JavaParser when resolving a method call
   * referring to a known annotation member declaration.
   *
   * @param ex The exception
   * @param node The node
   * @return The resolved annotation member declaration
   * @throws UnsupportedOperationException when the UnsupportedOperationException comes from an
   *     issue Specimin does not know how to address
   */
  private static ResolvedAnnotationMemberDeclaration handleUnsupportedOperationException(
      UnsupportedOperationException ex, Node node) throws UnsupportedOperationException {
    if (fqnToCompilationUnits == null) {
      throw new UnsupportedOperationException(
          "fqnToCompilationUnits must be set before calling handleUnsupportedOperationException");
    }

    // JavaParser bug: cannot resolve a method if in an annotation declaration

    // Annotation methods have no parameters
    // (https://docs.oracle.com/javase/specs/jls/se8/html/jls-9.html#jls-9.6.1)

    // Annotation methods also must have scope, since they cannot be super classes or
    // contain logic within their declaration.
    if (node instanceof MethodCallExpr methodCallExpr
        && methodCallExpr.getArguments().isEmpty()
        && methodCallExpr.hasScope()) {
      ResolvedType scope = calculateResolvedType(methodCallExpr.getScope().get());

      if (scope != null
          && scope.isReferenceType()
          && scope.asReferenceType().getTypeDeclaration().isPresent()) {
        ResolvedTypeDeclaration typeDecl = scope.asReferenceType().getTypeDeclaration().get();
        if (typeDecl.isAnnotation()) {
          ResolvedAnnotationDeclaration annotationDecl = typeDecl.asAnnotation();

          for (ResolvedAnnotationMemberDeclaration annotationMember :
              annotationDecl.getAnnotationMembers()) {
            if (annotationMember.getName().equals(methodCallExpr.getNameAsString())) {
              return annotationMember;
            }
          }
        }
      }
    }
    throw ex;
  }
}
