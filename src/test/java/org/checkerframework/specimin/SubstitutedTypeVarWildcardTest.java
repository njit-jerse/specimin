package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * Like {@link CallOnSubstitutedTypeVarTest}, but the type argument is a wildcard. Capture
 * conversion (JLS 5.1.10) gives {@code c} type {@code Container<CAP>} where {@code CAP} has upper
 * bound {@code Absent}, so {@code c.get()} has type {@code CAP}, and JLS 4.4 says the members
 * available on it are those of its bound. {@code absentMethod} therefore belongs on the synthetic
 * {@code Absent}, not on a type invented for the wildcard.
 */
public class SubstitutedTypeVarWildcardTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "substitutedtypevarwildcard",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar(Container<? extends Absent>)"});
  }
}
