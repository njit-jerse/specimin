package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/**
 * This test is a minimized version of a crash-inducing input derived from
 * https://github.com/uber/nullaway/issues/176.
 */
public class NA176Test {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "na176", new String[] {"com/example/Foo.java"}, new String[] {"com.example.Foo#foo()"});
  }
}
