package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks that if the targetted method in a Java file uses a global variable, the global
 * variable will be included in the minimized version of the Java file.
 */
public class HiddenTypeTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTest(
            "hiddenType",
            new String[] {"com/example/Simple.java"},
            new String[] {"com.example.Simple#isVoidType(MethodDeclaration)"});
  }
}
