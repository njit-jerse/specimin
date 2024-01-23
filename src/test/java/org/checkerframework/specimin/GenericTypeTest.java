package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/** This test checks if Specimin can handle simple generic type expressions. */
public class GenericTypeTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "generictype",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#Simple(T)"});
  }
}
