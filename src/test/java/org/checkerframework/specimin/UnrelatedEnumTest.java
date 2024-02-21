package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks that the presence of an enum in an unrelated target class (Foo) does not cause
 * that class to be preserved.
 */
public class UnrelatedEnumTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "unrelatedenum",
        new String[] {"com/example/Simple.java", "com/example/Foo.java"},
        new String[] {"com.example.Simple#bar()"});
  }
}
