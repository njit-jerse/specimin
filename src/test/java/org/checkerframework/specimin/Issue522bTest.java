package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * The variant of issue 522 whose array element type is generic but not a wildcard. The description
 * Specimin used to name it by, {@code com.example.Foo<java.lang.String>[]}, passes
 * FullyQualifiedNameSet's wildcard check, so this shape used to fail later instead: nothing is
 * registered under that name, and looking it up threw.
 */
public class Issue522bTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "issue522b",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#Simple(Foo<String>[])"});
  }
}
