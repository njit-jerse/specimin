package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/** This test checks that Specimin will properly remove unused extended interfaces. */
public class InterfaceExtendedTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "interfaceextended",
        new String[] {"com/example/Foo.java"},
        new String[] {"com.example.Foo#printMethod()"});
  }
}
