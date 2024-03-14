package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks that Specimin can handle complex cases involving anonymous classes and what's
 * in-scope inside them. Based on cf-3021 integration test.
 */
public class AnonClassScopeTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "anonclassscope",
        new String[] {
          "com/example/Simple.java",
          "com/example/ForwardingObject.java",
          "com/example/ForwardingMapEntry.java",
          "com/example/ForwardingMap.java"
        },
        new String[] {"com.example.Simple#checkedEntry(Entry<Class<? extends B>, B>)"});
  }
}
