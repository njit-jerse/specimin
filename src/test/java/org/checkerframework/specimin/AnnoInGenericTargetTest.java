package org.checkerframework.specimin;

import org.junit.Test;

import java.io.IOException;

/**
 * This test checks that TargetMethodFinderVisitor can correctly find the target method when
 * there is an annotation in a generic type in the original source. This is a regression test.
 */
public class AnnoInGenericTargetTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "annoingenerictarget",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar(Collection<? extends T>)"});
  }
}
