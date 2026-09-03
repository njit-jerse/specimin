package org.checkerframework.specimin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javaparser.ast.Node;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * Checks that {@code Resolver}'s set of node kinds whose resolution can widen stays in step with
 * the {@code Object}-returning overloads that force callers of those kinds to check the runtime
 * type.
 *
 * <p>The two can drift apart in either direction, and both directions are silent bugs. A kind added
 * to the set without an overload leaves the generic {@code <T> T} signature promising a {@code T}
 * it does not deliver, which is a {@code ClassCastException} at some caller. An overload without a
 * corresponding entry in the set means the widened value is discarded before it reaches the caller
 * that the overload was added for, so the widening silently stops working.
 */
public class ResolverKindContractTest {

  /**
   * Returns the node kinds that {@code Resolver} sanctions as able to resolve to something other
   * than their declared kind.
   *
   * @return that set
   * @throws ReflectiveOperationException if the field cannot be read
   */
  @SuppressWarnings("unchecked")
  private Set<Class<? extends Node>> sanctionedKinds() throws ReflectiveOperationException {
    Field field = Resolver.class.getDeclaredField("WIDENED_NODE_KINDS");
    field.setAccessible(true);
    return (Set<Class<? extends Node>>) field.get(null);
  }

  /**
   * Returns the parameter types of the overloads of {@code name} that return {@code Object}.
   *
   * @param name The method name to look for
   * @return the node kinds those overloads accept
   */
  private Set<Class<?>> objectReturningOverloads(String name) {
    Set<Class<?>> result = new LinkedHashSet<>();
    for (Method method : Resolver.class.getDeclaredMethods()) {
      if (method.getName().equals(name)
          && method.getReturnType() == Object.class
          && method.getParameterCount() == 1
          && Node.class.isAssignableFrom(method.getParameterTypes()[0])) {
        result.add(method.getParameterTypes()[0]);
      }
    }
    return result;
  }

  @Test
  public void resolveOverloadsCoverEverySanctionedKind() throws ReflectiveOperationException {
    assertEquals(
        sanctionedKinds(),
        objectReturningOverloads("resolve"),
        "Resolver.resolve's Object-returning overloads must match WIDENED_NODE_KINDS exactly");
  }

  @Test
  public void resolveGuaranteeNonNullOverloadsCoverEverySanctionedKind()
      throws ReflectiveOperationException {
    assertEquals(
        sanctionedKinds(),
        objectReturningOverloads("resolveGuaranteeNonNull"),
        "Resolver.resolveGuaranteeNonNull's Object-returning overloads must match"
            + " WIDENED_NODE_KINDS exactly");
  }

  @Test
  public void everySanctionedKindActuallyDeclaresANarrowerResolvedKind()
      throws ReflectiveOperationException {
    // A kind belongs in the set only because its Resolvable type argument is narrower than what
    // resolution can produce. One that resolves to Object already would be pointless to list.
    for (Class<? extends Node> kind : sanctionedKinds()) {
      Method method = Resolver.class.getDeclaredMethod("declaredResolvedKind", Class.class);
      method.setAccessible(true);
      Object declared = method.invoke(null, kind);
      assertTrue(
          declared != null && declared != Object.class,
          kind.getSimpleName()
              + " is listed as a widening node kind but does not declare a Resolvable type"
              + " argument, so nothing about it can widen");
    }
  }

  @Test
  public void sanctionedKindsAreDocumented() throws ReflectiveOperationException {
    // Guards against quietly adding a kind: the set is small and each entry needs a JLS-grounded
    // reason on WIDENED_NODE_KINDS, so a change here should be a deliberate, reviewed one.
    assertEquals(
        Set.of(
            "EnumConstantDeclaration",
            "MethodCallExpr",
            "MethodReferenceExpr",
            "ObjectCreationExpr"),
        sanctionedKinds().stream().map(Class::getSimpleName).collect(Collectors.toSet()),
        "Adding or removing a widening node kind means updating the reasons documented on"
            + " Resolver.WIDENED_NODE_KINDS and this test together");
  }
}
