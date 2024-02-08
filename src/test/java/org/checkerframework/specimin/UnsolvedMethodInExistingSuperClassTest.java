package org.checkerframework.specimin;

import org.junit.Test;

import java.io.IOException;

/**
 * This test checks if Specimin can properly update the list of target files to be solved by
 * UnsolvedSymbolVisitor in case there is an unsolved method from a superclass that is not added as a
 * target file.
 */
public class UnsolvedMethodInExistingSuperClassTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "unsolvedmethodinexistingsuperclass",
        new String[] {"com/example/Foo.java"},
        new String[] {"com.example.Foo#bar()"});
  }
}
