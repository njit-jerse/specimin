package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks that Specimin properly handles the scoping of enum constants used in switch
 * statements and expressions, which have special (weird) rules.
 */
public class SwitchEnumConstantTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "switchenumconstant",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar(MyEnum)"});
  }
}
