package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks to see if methods implemented in a concrete class that implements an
 * interface/abstract class are preserved when only abstract super methods are called.
 */
public class AbstractSuperWithConcreteImplTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "abstractsuperwithconcreteimpl",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#foo()"});
  }
}
