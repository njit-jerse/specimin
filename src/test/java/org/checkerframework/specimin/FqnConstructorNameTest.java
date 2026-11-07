package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * This test checks that when a target constructs an unsolved class through its fully-qualified
 * name, the synthetic constructor is declared with the class's simple name. JLS 8.8 requires a
 * constructor's name to be the simple name of its class, so a qualified name there does not parse.
 */
public class FqnConstructorNameTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "fqnconstructorname",
        new String[] {"com/example/Foo.java"},
        new String[] {"com.example.Foo#bar()"});
  }
}
