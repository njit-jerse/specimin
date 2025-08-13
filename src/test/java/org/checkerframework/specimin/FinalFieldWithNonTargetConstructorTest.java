package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks that an initializer is added to a final field if it was originally set in a
 * non-target constructor.
 */
public class FinalFieldWithNonTargetConstructorTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "finalfieldwithnontargetconstructor",
        new String[] {"com/example/Foo.java"},
        new String[] {"com.example.Foo#bar()"});
  }
}
