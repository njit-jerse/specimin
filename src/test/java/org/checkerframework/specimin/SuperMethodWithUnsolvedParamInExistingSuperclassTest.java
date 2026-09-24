package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * This test checks that a {@code super.m(...)} call keeps {@code m} in the direct superclass when
 * that superclass is in the input but one of {@code m}'s parameter types is not, so that JavaParser
 * cannot resolve the call. JLS 15.12.1 makes the direct superclass the class to search for {@code
 * super.m}, so {@code Base#register} must be preserved. See issue 558.
 */
public class SuperMethodWithUnsolvedParamInExistingSuperclassTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "supermethodwithunsolvedparaminexistingsuperclass",
        new String[] {"com/example/Sub.java"},
        new String[] {"com.example.Sub#foo(Info)"});
  }
}
