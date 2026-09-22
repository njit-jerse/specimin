package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * This test checks that comparing the result of an unsolved method with the {@code null} literal
 * does not make {@code null} the method's return type. By JLS 15.21.3, {@code e == null} is legal
 * for every reference type of {@code e}, so the comparison does not constrain that type.
 */
public class NullComparisonReturnTypeTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "nullcomparisonreturntype",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar(Ctx)"});
  }
}
