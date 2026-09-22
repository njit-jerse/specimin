package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * This test is {@link QualifiedNestedStaticFieldScopeTest}, but with a scope nested two levels
 * deep, {@code Outer.Inner.Deeper}. The synthetic type must be placed in {@code
 * com.other.Outer.Inner}, not in a class {@code Inner} of a package named {@code Outer}. See issue
 * #559.
 */
public class QualifiedNestedDeeperScopeTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "qualifiednesteddeeperscope",
        new String[] {"com/example/Foo.java"},
        new String[] {"com.example.Foo#bar()"});
  }
}
