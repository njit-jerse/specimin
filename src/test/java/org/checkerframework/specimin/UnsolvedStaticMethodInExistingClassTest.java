package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks if Specimin can properly update the list of target files to be solved by
 * UnsolvedSymbolVisitor in case there is an unsolved static method from a class that is not added
 * as a target file.
 */
public class UnsolvedStaticMethodInExistingClassTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "unsolvedstaticmethodfromexistingclass",
        new String[] {"com/example/Foo.java"},
        new String[] {"com.example.Foo#bar()"});
  }
}
