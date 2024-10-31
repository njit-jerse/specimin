package org.anonymous.typeslice;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks if TypeSlice can properly update the list of target files to be solved by
 * UnsolvedSymbolVisitor in case there is an unsolved method from a superclass that is not added as
 * a target file.
 */
public class UnsolvedMethodInExistingSuperClassTest {
  @Test
  public void runTest() throws IOException {
    TypeSliceTestExecutor.runTestWithoutJarPaths(
        "unsolvedmethodinexistingsuperclass",
        new String[] {"com/example/Foo.java"},
        new String[] {"com.example.Foo#bar()"});
  }
}
