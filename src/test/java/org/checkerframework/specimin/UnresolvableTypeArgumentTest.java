package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * This test checks that an in-project type named in a type that also has an unresolvable type
 * argument is still preserved. {@code Container} here is the direct superinterface of the target
 * method's enclosing class, so JLS 8.1.5 requires it to be declared for the output to compile; the
 * unresolvable type argument must not cause Specimin to lose track of it.
 */
public class UnresolvableTypeArgumentTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "unresolvabletypeargument",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar()"});
  }
}
