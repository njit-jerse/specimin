package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * Like {@link CallOnSubstitutedTypeVarTest}, but the receiver is a class instance creation
 * expression instead of a variable. The type arguments come from the {@code new} expression itself
 * (JLS 15.9.1), so {@code new Container<Absent>().get()} has type {@code Absent}. {@code Container}
 * has a default constructor that is already in the input, so no constructor should be synthesized
 * for it -- only the unresolvable type argument is missing.
 */
public class SubstitutedTypeVarInConstructorTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "substitutedtypevarinconstructor",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar()"});
  }
}
