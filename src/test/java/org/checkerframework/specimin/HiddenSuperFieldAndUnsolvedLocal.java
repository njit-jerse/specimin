package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks that if Specimin will work properly where there is a super variables call while
 * the parent class file is not in the root directory physically
 */
public class HiddenSuperFieldAndUnsolvedLocal {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "HiddenSuperFieldAndUnsolvedLocal",
        new String[] {"com/example/Child.java"},
        new String[] {"com.example.Child#returnLocalName()"});
  }
}
