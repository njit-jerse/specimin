package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/** A variant of issue 522 whose array element type is generic but not a wildcard. */
public class Issue522bTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "issue522b",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#Simple(Foo<String>[])"});
  }
}
