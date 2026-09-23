package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * This test checks that a synthetic type whose name places it in a nested class, here {@code
 * com.other.Outer.Inner}, is emitted inside that class and not inside {@code com.other.Outer},
 * which is also generated. The field's type is written {@code
 * com.other.Outer.Inner.ComOtherOuterInnerDeeperUPSyntheticType}, so it resolves only if that class
 * is a member of {@code Inner} (JLS 6.5.5.2).
 */
public class FullyQualifiedDeeperNestedScopeTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "fullyqualifieddeepernestedscope",
        new String[] {"com/example/Foo.java"},
        new String[] {"com.example.Foo#bar()"});
  }
}
