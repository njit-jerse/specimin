package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * Like {@link CallOnSubstitutedTypeVarTest}, but the type variable is declared by the method rather
 * than by the declaring class, and is supplied by an explicit type argument at the call site (JLS
 * 15.12.2.1). The type of {@code Factory.<Absent>make()} is therefore {@code Absent}, and {@code
 * absentMethod} is a member of the synthetic {@code Absent}.
 */
public class SubstitutedMethodTypeVarTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "substitutedmethodtypevar",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar()"});
  }
}
