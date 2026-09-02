package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * Like {@link CallOnSubstitutedTypeVarTest}, but the type variable is the component type of an
 * array. Substitution distributes over array types (JLS 4.5.2, 10.1): {@code T[]} under [T :=
 * Absent] is {@code Absent[]}, whose component type is {@code Absent}, so {@code absentMethod} is a
 * member of the synthetic {@code Absent}.
 */
public class SubstitutedTypeVarArrayTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "substitutedtypevararray",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar(Container<Absent>)"});
  }
}
