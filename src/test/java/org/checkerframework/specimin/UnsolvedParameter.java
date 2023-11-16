package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/** This test checks if Specimin can work for tricky, unsolved parameters. */
public class UnsolvedParameter {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "unsolvedparameter",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#lengthImportStatement(NodeList<ImportDeclaration>)"});
  }
}
