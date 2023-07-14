package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks when two simple, unrelated classes are given as input, only the target is
 * preserved.
 */
public class UnusedSecondClassTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTest(
        "unusedsecondclass",
        new String[] {"com/example/Foo.java", "com/example/Baz.java"},
        new String[] {"com.example.Foo#bar()"});
  }
}
