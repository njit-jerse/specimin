package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * Like {@link CallOnSubstitutedTypeVarTest}, but the substituted type variable is the type of a
 * field rather than a method's return type. By JLS 4.5.2 the type of {@code c.item} where {@code c}
 * has type {@code Container<Absent>} is the result of applying [T := Absent] to {@code T}, so
 * {@code absentMethod} is a member of the synthetic {@code Absent}. {@code Container#item} is read
 * here, so JLS 15.11.1 requires its declaration to survive.
 */
public class FieldOfSubstitutedTypeVarTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "fieldofsubstitutedtypevar",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar(Container<Absent>)"});
  }
}
