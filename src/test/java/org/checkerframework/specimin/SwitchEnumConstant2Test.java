package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks that Specimin properly handles the scoping of enum constants used in switch
 * statements and expressions, which have special (weird) rules. This variant checks that only
 * constants used in the case part of the switch go into the enum.
 */
public class SwitchEnumConstant2Test {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "switchenumconstant2",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar(MyEnum)"});
  }
}
