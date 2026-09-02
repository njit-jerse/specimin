package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * This test checks that a method call whose receiver is a call to a generic method is handled when
 * the receiver's type comes from substituting an unsolved type for a type variable. In {@code
 * existingLease.getHolder().getStatus()}, {@code Lease#getHolder} is declared to return the type
 * variable {@code T}, so by JLS 4.5.2 the type of {@code existingLease.getHolder()} is the result
 * of applying the substitution [T := InstanceInfo] induced by the receiver's type {@code
 * Lease<InstanceInfo>}. {@code InstanceInfo} is not in the input (in the program this test is
 * reduced from, it lives in a separate module), so {@code getStatus} must be synthesized as a
 * member of a synthetic {@code InstanceInfo} -- not of {@code T}, which names no type at all.
 *
 * <p>The return type of the synthetic {@code getStatus} is unconstrained: JLS 14.8 permits a method
 * invocation statement of any return type, so a placeholder type is used.
 */
public class CallOnSubstitutedTypeVarTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "callonsubstitutedtypevar",
        new String[] {"com/netflix/eureka/registry/rule/LeaseExistsRule.java"},
        new String[] {
          "com.netflix.eureka.registry.rule.LeaseExistsRule#apply(Lease<InstanceInfo>)"
        });
  }
}
