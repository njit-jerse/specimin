package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * Checks that a substitution composes across two generic declarations: {@code Outer<Absent>} makes
 * {@code inner()} return {@code Inner<Absent>} (JLS 4.5.2), and that result must in turn make
 * {@code get()} return {@code Absent} rather than {@code Inner}'s own type variable.
 *
 * <p>The composition happens because the type computed for the receiver of the second call carries
 * the type arguments substituted into the first, so each link needs only one level of substitution.
 */
public class SubstitutedTypeVarChainedTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "substitutedtypevarchained",
        new String[] {
          "com/example/Simple.java", "com/example/Outer.java", "com/example/Inner.java"
        },
        new String[] {"com.example.Simple#bar(Outer<Absent>)"});
  }
}
