package org.checkerframework.specimin;

import com.github.javaparser.ast.AccessSpecifier;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.MethodReferenceExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithArguments;
import com.github.javaparser.ast.nodeTypes.NodeWithParameters;
import com.github.javaparser.ast.nodeTypes.NodeWithTraversableScope;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.MethodAmbiguityException;
import com.github.javaparser.resolution.Resolvable;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedAnnotationDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedAnnotationMemberDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodLikeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedParameterDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedTypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedTypeParameterDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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
   * Node kinds whose resolution can legitimately produce a declaration other than the one their
   * {@code Resolvable<T>} type argument names, because JavaParser has no node for the declaration
   * that is actually referred to. A record's constructor call names the record declaration, since a
   * canonical constructor is implicitly declared (JLS 8.10.4); an enum constant names the enum's
   * constructor; a constructor reference names a constructor rather than a method; and a call on an
   * annotation member names that member (JLS 9.6.1), which is a value, not a method.
   *
   * <p>{@link #resolve} and {@link #resolveGuaranteeNonNull} are overloaded to return {@code
   * Object} for exactly these kinds, so that callers are forced to dispatch on the runtime type.
   * Any other kind of mismatch is rejected by {@link #discardIfWrongKind}, which keeps the generic
   * signature honest. Adding a widening recovery path for a new node kind means adding the kind
   * here <em>and</em> adding the matching overloads; {@code ResolverKindContractTest} enforces that
   * the two stay in step.
   */
  private static final Set<Class<? extends Node>> WIDENED_NODE_KINDS =
      Set.of(
          ObjectCreationExpr.class,
          MethodCallExpr.class,
          MethodReferenceExpr.class,
          EnumConstantDeclaration.class);

  /** Cache for {@link #declaredResolvedKind}, which is reflective and called per recovery. */
  private static final Map<Class<?>, Optional<Class<?>>> DECLARED_RESOLVED_KINDS =
      new ConcurrentHashMap<>();

  /**
   * Returns the type argument that {@code nodeClass} supplies to {@link Resolvable}, that is, the
   * kind of declaration (or type) that resolving such a node is declared to produce.
   *
   * @param nodeClass The class of the node being resolved
   * @return That type argument, or null if it cannot be determined
   */
  private static @Nullable Class<?> declaredResolvedKind(Class<?> nodeClass) {
    return DECLARED_RESOLVED_KINDS
        .computeIfAbsent(nodeClass, Resolver::computeDeclaredResolvedKind)
        .orElse(null);
  }

  /**
   * Computes the value cached by {@link #declaredResolvedKind}, by walking {@code nodeClass}'s
   * supertypes for the {@link Resolvable} interface.
   *
   * @param nodeClass The class of the node being resolved
   * @return That type argument, or an empty Optional if it cannot be determined
   */
  private static Optional<Class<?>> computeDeclaredResolvedKind(Class<?> nodeClass) {
    for (Class<?> current = nodeClass; current != null; current = current.getSuperclass()) {
      for (java.lang.reflect.Type itf : current.getGenericInterfaces()) {
        if (itf instanceof ParameterizedType parameterized
            && parameterized.getRawType() == Resolvable.class
            && parameterized.getActualTypeArguments()[0] instanceof Class<?> argument) {
          return Optional.of(argument);
        }
      }
    }
    return Optional.empty();
  }

  /**
   * Discards a recovery-path result that is not what resolving {@code node} is declared to produce,
   * unless {@code node} is one of the {@link #WIDENED_NODE_KINDS}.
   *
   * <p>Without this, the unchecked cast in {@link #resolve} reaches the caller as a {@code
   * ClassCastException}. Null is the right answer for a discarded result rather than an exception:
   * a recovery path that produces the wrong kind of thing has not found the declaration, and "not
   * resolvable" is what every caller already handles. The type-versus-declaration case is the one
   * that motivated this check -- {@link JavaParserUtil#tryResolveNodeIfInAnonymousClass} falls back
   * to the node's <em>type</em>, which is no answer to "what declaration does this name?".
   *
   * @param node The node that was being resolved
   * @param result The result a recovery path produced, possibly null
   * @return {@code result}, or null if it is not a kind that {@code node} can resolve to
   */
  private static @Nullable Object discardIfWrongKind(Node node, @Nullable Object result) {
    if (result == null || WIDENED_NODE_KINDS.stream().anyMatch(kind -> kind.isInstance(node))) {
      return result;
    }
    Class<?> declared = declaredResolvedKind(node.getClass());
    return declared == null || declared.isInstance(result) ? result : null;
  }

  /**
   * Resolves a resolvable node. Use instead of {@link Resolvable#resolve()} because this handles
   * all known exceptions, and returns null when otherwise unresolvable.
   *
   * <p>Overloads returning {@code Object} shadow this method for the node kinds whose resolution
   * can widen; see {@link #WIDENED_NODE_KINDS}. For every other kind the result really is a {@code
   * T}, because {@link #discardIfWrongKind} drops anything else.
   *
   * @param toResolve The node to resolve
   * @return The resolved object, or null if not resolvable
   * @param <T> The type to resolve to
   */
  @SuppressWarnings("unchecked")
  // Safe: discardIfWrongKind has already dropped any result that is not a T, and the node kinds
  // for which it permits a wider result are shadowed by the Object-returning overloads below.
  public static <T> @Nullable T resolve(Resolvable<T> toResolve) {
    return (T) resolveWidened(toResolve);
  }

  /**
   * Resolves an object creation expression. Returns {@code Object} rather than a {@code
   * ResolvedConstructorDeclaration} because constructing a record resolves to the record's
   * declaration; see {@link #WIDENED_NODE_KINDS}.
   *
   * @param toResolve The node to resolve
   * @return The resolved object, or null if not resolvable
   */
  public static @Nullable Object resolve(ObjectCreationExpr toResolve) {
    return resolveWidened(toResolve);
  }

  /**
   * Resolves a method call. Returns {@code Object} rather than a {@code ResolvedMethodDeclaration}
   * because a call on an annotation member resolves to that member; see {@link
   * #WIDENED_NODE_KINDS}.
   *
   * @param toResolve The node to resolve
   * @return The resolved object, or null if not resolvable
   */
  public static @Nullable Object resolve(MethodCallExpr toResolve) {
    return resolveWidened(toResolve);
  }

  /**
   * Resolves a method reference. Returns {@code Object} rather than a {@code
   * ResolvedMethodDeclaration} because a constructor reference resolves to a constructor; see
   * {@link #WIDENED_NODE_KINDS}.
   *
   * @param toResolve The node to resolve
   * @return The resolved object, or null if not resolvable
   */
  public static @Nullable Object resolve(MethodReferenceExpr toResolve) {
    return resolveWidened(toResolve);
  }

  /**
   * Resolves an enum constant. Returns {@code Object} rather than a {@code
   * ResolvedEnumConstantDeclaration} because an enum constant resolves to the enum's constructor
   * when its arguments are unresolvable; see {@link #WIDENED_NODE_KINDS}.
   *
   * @param toResolve The node to resolve
   * @return The resolved object, or null if not resolvable
   */
  public static @Nullable Object resolve(EnumConstantDeclaration toResolve) {
    return resolveWidened(toResolve);
  }

  /**
   * Implementation of every {@code resolve} overload: resolves the node, recovering from the
   * exceptions JavaParser throws for cases it cannot handle.
   *
   * @param toResolve The node to resolve
   * @return The resolved object, or null if not resolvable
   */
  private static @Nullable Object resolveWidened(Resolvable<?> toResolve) {
    if (fqnToCompilationUnits == null) {
      throw new UnsupportedOperationException(
          "fqnToCompilationUnits must be set before calling resolve");
    }

    Node node = (Node) toResolve;
    try {
      return toResolve.resolve();
    } catch (UnsolvedSymbolException ex) {
      return discardIfWrongKind(node, tryAlternativeResolutionForUnsolvableNode(node));
    } catch (IllegalStateException ex) {
      return discardIfWrongKind(node, Resolver.handleIllegalStateException(ex, node));
    } catch (MethodAmbiguityException ex) {
      return discardIfWrongKind(node, Resolver.handleMethodAmbiguityException(ex, node));
    } catch (UnsupportedOperationException ex) {
      return discardIfWrongKind(node, Resolver.handleUnsupportedOperationException(ex, node));
    }
  }

  /**
   * Resolves a resolvable node. Use instead of {@link Resolvable#resolve()} because this handles
   * all known exceptions. Use when you are sure that the node is resolvable, and want to throw an
   * exception if it is not. Typically used with declarations, since those are almost always
   * solvable.
   *
   * <p>As with {@link #resolve}, overloads returning {@code Object} shadow this method for the node
   * kinds whose resolution can widen; see {@link #WIDENED_NODE_KINDS}.
   *
   * @param toResolve The node to resolve
   * @return The resolved object
   * @param <T> The type to resolve to
   */
  @SuppressWarnings("unchecked")
  // Safe for the same reason as in resolve: anything that is not a T has already been discarded.
  public static <T> @NonNull T resolveGuaranteeNonNull(Resolvable<T> toResolve) {
    return (@NonNull T) resolveGuaranteeNonNullWidened(toResolve);
  }

  /**
   * Resolves an object creation expression, throwing if it is not resolvable. Returns {@code
   * Object} for the reason given on {@link #resolve(ObjectCreationExpr)}.
   *
   * @param toResolve The node to resolve
   * @return The resolved object
   */
  public static @NonNull Object resolveGuaranteeNonNull(ObjectCreationExpr toResolve) {
    return resolveGuaranteeNonNullWidened(toResolve);
  }

  /**
   * Resolves a method call, throwing if it is not resolvable. Returns {@code Object} for the reason
   * given on {@link #resolve(MethodCallExpr)}.
   *
   * @param toResolve The node to resolve
   * @return The resolved object
   */
  public static @NonNull Object resolveGuaranteeNonNull(MethodCallExpr toResolve) {
    return resolveGuaranteeNonNullWidened(toResolve);
  }

  /**
   * Resolves a method reference, throwing if it is not resolvable. Returns {@code Object} for the
   * reason given on {@link #resolve(MethodReferenceExpr)}.
   *
   * @param toResolve The node to resolve
   * @return The resolved object
   */
  public static @NonNull Object resolveGuaranteeNonNull(MethodReferenceExpr toResolve) {
    return resolveGuaranteeNonNullWidened(toResolve);
  }

  /**
   * Resolves an enum constant, throwing if it is not resolvable. Returns {@code Object} for the
   * reason given on {@link #resolve(EnumConstantDeclaration)}.
   *
   * @param toResolve The node to resolve
   * @return The resolved object
   */
  public static @NonNull Object resolveGuaranteeNonNull(EnumConstantDeclaration toResolve) {
    return resolveGuaranteeNonNullWidened(toResolve);
  }

  /**
   * Implementation of every {@code resolveGuaranteeNonNull} overload.
   *
   * @param toResolve The node to resolve
   * @return The resolved object
   */
  private static @NonNull Object resolveGuaranteeNonNullWidened(Resolvable<?> toResolve) {
    Node node = (Node) toResolve;
    Object result;

    try {
      result = toResolve.resolve();
    } catch (UnsolvedSymbolException ex) {
      Object resolved = discardIfWrongKind(node, tryAlternativeResolutionForUnsolvableNode(node));

      if (resolved == null) {
        throw ex;
      }

      result = resolved;
    } catch (IllegalStateException ex) {
      Object resolved = discardIfWrongKind(node, Resolver.handleIllegalStateException(ex, node));

      if (resolved == null) {
        throw ex;
      }

      result = resolved;
    } catch (MethodAmbiguityException ex) {
      Object resolved = discardIfWrongKind(node, Resolver.handleMethodAmbiguityException(ex, node));

      if (resolved == null) {
        throw ex;
      }

      result = resolved;
    } catch (UnsupportedOperationException ex) {
      Object resolved =
          discardIfWrongKind(node, Resolver.handleUnsupportedOperationException(ex, node));

      if (resolved == null) {
        throw ex;
      }

      result = resolved;
    }

    if (result == null) {
      // This should never happen, but it's here to satisfy the null checker
      throw new RuntimeException("Resolved result was null");
    }

    return result;
  }

  /**
   * Attempts to resolve the erasure of a parameterized type whose type arguments make it
   * unresolvable. {@link Resolvable#resolve()} on a {@link ClassOrInterfaceType} converts its type
   * arguments as well, so any unresolvable type argument makes the whole type unresolvable, even if
   * the erasure would be resolvable. This method constructs the erasure and tries to resolve it,
   * returning non-null only if that succeeds.
   *
   * @param type The type whose erasure to resolve
   * @return The resolved erasure, or null if it cannot be resolved (or if it has no type arguments
   *     to erase)
   */
  public static @Nullable ResolvedType resolveErasure(ClassOrInterfaceType type) {
    Optional<NodeList<Type>> typeArguments = type.getTypeArguments();
    if (typeArguments.isEmpty() || typeArguments.get().isEmpty()) {
      return null;
    }

    // Mutating and restoring the node is ugly, but a clone has no parent and therefore no
    // resolution context. JavaParserUtil#tryGetErasedTypeOfNode does the same thing.
    type.removeTypeArguments();
    ResolvedType erasure;
    try {
      erasure = resolve(type);
    } finally {
      type.setTypeArguments(typeArguments.get());
    }
    return erasure;
  }

  /**
   * Attempts to resolve a node that fails to resolve only because of an unsolved type argument.
   *
   * @param toResolve The node to resolve
   * @return the resolved node names, or null if erasing type arguments does not recover it
   */
  public static @Nullable Object resolveThroughErasure(Resolvable<?> toResolve) {
    if (toResolve instanceof ClassOrInterfaceType type) {
      return resolveErasure(type);
    }
    return resolveThroughErasedReceiver(toResolve);
  }

  /**
   * Attempts to resolve an expression that names a member of a type whose type arguments make that
   * type unresolvable. A member access does not resolve when its receiver's type does not, but JLS
   * 4.5.2 makes the members of a parameterized type the members of the generic declaration with a
   * substitution applied -- so the access names the same declaration on the parameterized type as
   * it does on the erasure. This erases the receiver's declared type and tries again, returning
   * non-null only if that succeeds.
   *
   * @param expr The member access to resolve
   * @return The declaration that the access names, or null if it cannot be found this way
   * @param <T> The type to resolve to
   */
  private static <T> @Nullable T resolveThroughErasedReceiver(Resolvable<T> expr) {
    if (!(expr instanceof Expression original)
        || !(expr instanceof NodeWithTraversableScope)
        || original.getParentNode().isEmpty()) {
      return null;
    }

    // Resolve a clone standing in for the expression, rather than the expression itself: resolving
    // it while the receiver's type is erased would leave JavaParser's caches holding the erased
    // answer for the real node, which every later resolution of it would then see.
    Expression standIn = original.clone();
    original.replace(standIn);
    ClassOrInterfaceType receiverType = null;
    NodeList<Type> typeArguments = null;
    try {
      receiverType =
          getDeclaredTypeOfReceiver(
              ((NodeWithTraversableScope) standIn).traverseScope().orElse(null));
      if (receiverType == null
          || receiverType.getTypeArguments().isEmpty()
          || receiverType.getTypeArguments().get().isEmpty()) {
        return null;
      }
      typeArguments = receiverType.getTypeArguments().get();
      receiverType.removeTypeArguments();

      @SuppressWarnings(
          "unchecked") // standIn is a clone of expr, so it resolves to the same thing.
      Resolvable<T> asResolvable = (Resolvable<T>) standIn;
      return resolve(asResolvable);
    } finally {
      if (receiverType != null && typeArguments != null) {
        receiverType.setTypeArguments(typeArguments);
      }
      standIn.replace(original);
    }
  }

  /**
   * Returns the declared type of a receiver expression, as written in the AST, or null if it is not
   * one that names a type whose type arguments can be erased in place.
   *
   * @param receiver The receiver expression, or null
   * @return The receiver's declared type, or null
   */
  private static @Nullable ClassOrInterfaceType getDeclaredTypeOfReceiver(
      @Nullable Expression receiver) {
    if (fqnToCompilationUnits == null) {
      throw new UnsupportedOperationException(
          "fqnToCompilationUnits must be set before calling resolve");
    }
    if (receiver instanceof ObjectCreationExpr creation) {
      return creation.getType();
    }
    if (!(receiver instanceof Resolvable<?> resolvable)) {
      return null;
    }
    Object resolvedReceiver = resolve(resolvable);
    if (!(resolvedReceiver instanceof ResolvedValueDeclaration valueDeclaration)) {
      return null;
    }
    Type declaredType =
        JavaParserUtil.getTypeFromResolvedValueDeclaration(valueDeclaration, fqnToCompilationUnits);
    return declaredType instanceof ClassOrInterfaceType asClass ? asClass : null;
  }

  /**
   * Tries alternative resolution strategies for a node that cannot be resolved through JavaParser's
   * symbol solver. Callers are responsible for passing the result through {@link
   * #discardIfWrongKind}: some of these strategies answer with the node's type rather than with a
   * declaration.
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
    }

    // Nothing resolves inside an anonymous class whose supertype is unsolvable, because
    // JavaParser builds a JavaParserAnonymousClassDeclaration (which resolves that supertype)
    // before it looks up the name. For example, even a java.lang type named in the body of such
    // is considered "unsolvable" by JavaParser. We only retry expressions and types, because those
    // denote the same thing wherever they are written; the alternative resolution strategy here
    // tries to hoist them out of the anonymous class and checks if they resolve there.
    if (unsolvable instanceof Expression || unsolvable instanceof Type) {
      Object resolvedOutsideAnonymousClass =
          JavaParserUtil.tryResolveNodeIfInAnonymousClass(unsolvable);

      if (resolvedOutsideAnonymousClass != null) {
        return resolvedOutsideAnonymousClass;
      }
    }

    if (unsolvable instanceof MethodCallExpr methodCallExpr) {
      Object result = handleUnresolvableRecordMember(methodCallExpr);

      if (result != null) {
        return result;
      }
    }

    // Handle cases where a method/constructor call cannot be resolved because of unresolvable
    // arguments, but its definition exists
    NodeWithParameters<?> potentiallyResolvableCallable =
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
      if (node instanceof MethodCallExpr methodCallExpr
          && JavaParserUtil.tryFindSingleCallableForNodeWithUnresolvableArguments(
                  methodCallExpr, fqnToCompilationUnits)
              instanceof MethodDeclaration methodDecl) {
        return methodDecl.resolve();
      }

      throw ex;
    }

    return null;
  }

  /**
   * Resolves record members because JavaParser can't.
   *
   * @param methodCallExpr The method call expression
   * @return The parameter representing the record member, or null if not found
   */
  private static @Nullable ResolvedMethodDeclaration handleUnresolvableRecordMember(
      MethodCallExpr methodCallExpr) {
    if (fqnToCompilationUnits == null) {
      throw new UnsupportedOperationException(
          "fqnToCompilationUnits must be set before calling handleUnsupportedOperationException");
    }

    if (methodCallExpr.getArguments().isNonEmpty()) {
      return null;
    }

    // JavaParser bug: cannot resolve MethodCallExpr if it represents a record member

    // We have to use our own implementation, ResolvedRecordMemberDeclaration, since JavaParser
    // throws an IllegalStateException anytime we try to resolve the original parameter.
    if (methodCallExpr.getArguments().isEmpty()) {
      if (methodCallExpr.hasScope()) {
        ResolvedType scopeType = calculateResolvedType(methodCallExpr.getScope().get());

        if (scopeType == null
            || !scopeType.isReferenceType()
            || scopeType.asReferenceType().getTypeDeclaration().isEmpty()
            || !scopeType.asReferenceType().getTypeDeclaration().get().isRecord()) {
          return null;
        }

        TypeDeclaration<?> typeDecl =
            JavaParserUtil.getTypeFromQualifiedName(scopeType.describe(), fqnToCompilationUnits);

        if (typeDecl != null) {
          for (Parameter param : typeDecl.asRecordDeclaration().getParameters()) {
            if (param.getNameAsString().equals(methodCallExpr.getNameAsString())) {
              return new ResolvedRecordMemberDeclaration(param);
            }
          }
        }
      } else {
        TypeDeclaration<?> encapsulating =
            JavaParserUtil.getEnclosingClassLikeOptional(methodCallExpr);

        while (encapsulating != null) {
          if (encapsulating.isRecordDeclaration()) {
            for (Parameter param : encapsulating.asRecordDeclaration().getParameters()) {
              if (param.getNameAsString().equals(methodCallExpr.getNameAsString())) {
                return new ResolvedRecordMemberDeclaration(param);
              }
            }
          }

          if (encapsulating.getParentNode().isEmpty()) {
            return null;
          }

          // Check all outer classes
          encapsulating =
              JavaParserUtil.getEnclosingClassLikeOptional(encapsulating.getParentNode().get());
        }
      }
    }

    return null;
  }

  /**
   * Handles an UnsupportedOperationException thrown by JavaParser when resolving a method call
   * referring to a known annotation member declaration, or a method reference whose target
   * functional interface JavaParser cannot determine.
   *
   * @param ex The exception
   * @param node The node
   * @return The resolved declaration, or null if the node is a method reference that should be
   *     handled by synthesizing an unsolved symbol instead
   * @throws UnsupportedOperationException when the UnsupportedOperationException comes from an
   *     issue Specimin does not know how to address
   */
  private static @Nullable Object handleUnsupportedOperationException(
      UnsupportedOperationException ex, Node node) throws UnsupportedOperationException {
    if (fqnToCompilationUnits == null) {
      throw new UnsupportedOperationException(
          "fqnToCompilationUnits must be set before calling handleUnsupportedOperationException");
    }

    // JavaParser bug: MethodReferenceExprContext#inferArgumentTypes only knows how to find the
    // target functional interface of a method reference when the reference's parent node is a
    // method call, an object creation, a variable declarator, or a return statement. For every
    // other parent -- a cast, for instance -- it throws a bare UnsupportedOperationException,
    // and constructor references always throw regardless of the parent. Specimin does not need
    // the target functional interface to find the referenced method, though: the scope's type
    // determines it, so resolve it from the scope instead.
    if (node instanceof MethodReferenceExpr methodRef) {
      List<? extends ResolvedMethodLikeDeclaration> candidates =
          JavaParserUtil.getMethodDeclarationsFromMethodRef(methodRef);

      // With more than one candidate the target functional interface would be needed to choose
      // between the overloads, and that is exactly what is unavailable here. Returning null (as
      // when the scope's type is unsolvable and there are no candidates at all) leaves the caller
      // to preserve every candidate; see Slicer#preserveAmbiguousMethodRefCandidates.
      //
      // Note that for a constructor reference this returns a ResolvedConstructorDeclaration, even
      // though MethodReferenceExpr is declared as Resolvable<ResolvedMethodDeclaration>. Every
      // consumer of a resolved node takes it as an Object and dispatches on its runtime type, so
      // the wider return type is safe; do not narrow a resolved value to ResolvedMethodDeclaration
      // without checking it first.
      return candidates.size() == 1 ? candidates.get(0) : null;
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

  /**
   * Custom class to hold a record member declaration, since JavaParser cannot handle this case.
   *
   * @param parameter The wrapped parameter.
   */
  private record ResolvedRecordMemberDeclaration(Parameter parameter)
      implements ResolvedMethodDeclaration {
    @Override
    public ResolvedType getReturnType() {
      return Resolver.resolveGuaranteeNonNull(parameter.getType());
    }

    @Override
    public boolean isAbstract() {
      return false;
    }

    @Override
    public boolean isDefaultMethod() {
      return false;
    }

    @Override
    public boolean isStatic() {
      return false;
    }

    @Override
    public String toDescriptor() {
      return "";
    }

    @Override
    public ResolvedReferenceTypeDeclaration declaringType() {
      return (ResolvedReferenceTypeDeclaration)
          resolveGuaranteeNonNull((Resolvable<?>) JavaParserUtil.getEnclosingClassLike(parameter));
    }

    @Override
    public int getNumberOfParams() {
      return 0;
    }

    @Override
    public ResolvedParameterDeclaration getParam(int i) {
      throw new IndexOutOfBoundsException(
          "There are no parameters in a record member declaration.");
    }

    @Override
    public int getNumberOfSpecifiedExceptions() {
      return 0;
    }

    @Override
    public ResolvedType getSpecifiedException(int index) {
      throw new IndexOutOfBoundsException(
          "There are no exceptions in a record member declaration.");
    }

    @Override
    public AccessSpecifier accessSpecifier() {
      return AccessSpecifier.PUBLIC;
    }

    @Override
    public String getName() {
      return parameter.getNameAsString();
    }

    @Override
    public List<ResolvedTypeParameterDeclaration> getTypeParameters() {
      return List.of();
    }

    @Override
    public Optional<Node> toAst() {
      return Optional.of(parameter);
    }
  }
}
