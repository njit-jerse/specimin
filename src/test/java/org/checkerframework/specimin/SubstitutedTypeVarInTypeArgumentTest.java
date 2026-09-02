package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * Like {@link CallOnSubstitutedTypeVarTest}, but the type variable is nested inside a solvable
 * generic type rather than standing alone: {@code List<T>} under [T := Absent] is {@code
 * List<Absent>} (JLS 4.5.2), so {@code get(0)} has type {@code Absent} and {@code absentMethod} is
 * a member of it.
 */
public class SubstitutedTypeVarInTypeArgumentTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "substitutedtypevarintypeargument",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar(Container<Absent>)"});
  }
}
