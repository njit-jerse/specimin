package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/** This test checks if Specimin can handle fields of type primitive array. */
public class PrimitiveArrayTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "primitivearray",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar()"});
  }
}
