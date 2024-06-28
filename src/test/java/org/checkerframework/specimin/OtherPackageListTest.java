package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/**
 * This test is a variant of {@link OtherPackageArray3Test} that uses a list instead of an array, to
 * test our handling of the component types of wildcards with bounds (whereas {@link
 * OtherPackageArray3Test} and its cousins are concerned with the component type of an array).
 */
public class OtherPackageListTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "otherpackagelist",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar(org.example.Method)"});
  }
}
