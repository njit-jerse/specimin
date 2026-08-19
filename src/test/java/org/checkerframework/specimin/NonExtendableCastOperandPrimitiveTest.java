package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * A cast to a synthetic type is made to compile by making that type a subtype of the operand's type
 * (JLS 15.20.2), but here the operand's type is {@code int}, which has no subtypes at all. The
 * requirement cannot be met on the synthetic type's side, so it is met on the other: the generated
 * method's return type becomes an unconstrained type variable, which {@code T} instantiates to
 * {@code Baz} at the cast and to {@code Integer} at the assignment.
 *
 * <p>Before this was handled, the dropped requirement left {@code Baz} declared as {@code extends
 * int}, which is not a Java program at all.
 *
 * <p>The repair happens at the cast rather than at the assignment because only the cast can see the
 * problem: by the time the assignment is examined, the return type may already be {@code int},
 * which satisfies the assignment and gives it no reason to act. {@link
 * NonExtendableCastOperandReversedTest} is the same program with the two statements swapped.
 */
public class NonExtendableCastOperandPrimitiveTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "nonextendablecastoperandprimitive",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#target(Foo)"});
  }
}
