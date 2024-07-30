package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/** This test checks if Specimin can handle jar files as input */
public class JarFileTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTest(
        "jarfile",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#test()"},
        "cf",
        new String[] {"src/test/resources/jarfile/input/Book.jar"});
  }
}
