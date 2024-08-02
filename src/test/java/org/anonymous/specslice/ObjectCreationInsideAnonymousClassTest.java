package org.anonymous.specslice;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks that if SpecSlice will work if there is an object creation inside an anonymous
 * class
 */
public class ObjectCreationInsideAnonymousClassTest {
  @Test
  public void runTest() throws IOException {
    SpecSliceTestExecutor.runTestWithoutJarPaths(
        "ObjectCreationInsideAnonymousClass",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#getBaz()"});
  }
}
