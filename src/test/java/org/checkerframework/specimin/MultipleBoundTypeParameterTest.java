package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/**
 * This test class checks if Specimin can handle a method scope with a type parameter that extends
 * multiple bounds (For example, "I extends Type1 & Type2").
 */
public class MultipleBoundTypeParameterTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "multipleboundtypeparameter",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar()"});
  }
}
