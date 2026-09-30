package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * This test checks that the abstract method of a functional interface on the source path is
 * preserved when a method reference targets it only through a cast (JLS 15.16, 15.13.2). See issue
 * #567.
 */
public class MethodRefSolvedInterfaceCastTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "methodrefsolvedinterfacecast",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#foo(boolean)"});
  }
}
