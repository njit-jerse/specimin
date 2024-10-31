package org.anonymous.typeslice;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks that TypeSlice can properly ignore annotations in the parameters of a target
 * method while comparing method signatures.
 */
public class ParameterWithAnnotationsTest {
  @Test
  public void runTest() throws IOException {
    TypeSliceTestExecutor.runTest(
        "parameterwithannotations",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar(byte[], UnsolvedType)"},
        "cf",
        new String[] {"src/test/resources/shared/checker-qual-3.42.0.jar"});
  }
}
