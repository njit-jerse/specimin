package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * {@link MemberAccessThenContextTypeTest} with {@code java.lang.Object} as the assignment target.
 *
 * <p>{@code Object} is a bound like any other for the purpose of not letting it outrank the member
 * access -- it is a type from the JDK, so {@code foo} cannot be declared on it -- but it is the one
 * bound that is never written down: every class extends it already, so it is not recorded as a
 * supertype of the synthetic type. The expected output therefore names no supertype at all, and the
 * synthetic type has to be kept on the strength of the member it carries rather than the supertype
 * it does not.
 */
public class ObjectContextThenMemberAccessTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "objectcontextthenmemberaccess",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar(Item)"});
  }
}
