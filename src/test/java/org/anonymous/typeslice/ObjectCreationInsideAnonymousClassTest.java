package org.anonymous.typeslice;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks that if TypeSlice will work if there is an object creation inside an anonymous
 * class
 */
public class ObjectCreationInsideAnonymousClassTest {
  @Test
  public void runTest() throws IOException {
    TypeSliceTestExecutor.runTestWithoutJarPaths(
        "ObjectCreationInsideAnonymousClass",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#getBaz()"});
  }
}
