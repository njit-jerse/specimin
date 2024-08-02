package org.anonymous.specslice;

import java.io.IOException;
import org.junit.Test;

/**
 * This test demonstrates the imprecision of SpecSlice when there are type inference required to get
 * the type of an exception precisely.
 */
public class ExceptionTypeInferenceImprecisionTest {
  @Test
  public void runTest() throws IOException {
    SpecSliceTestExecutor.runTestWithoutJarPaths(
        "ExceptionTypeInferenceImprecision",
        new String[] {"com/example/Foo.java"},
        new String[] {"com.example.Foo#bar()"});
  }
}
