package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks that if a class extends an abstract class or implements an interface, the
 * methods that need to be implemented still are (to preserve compilability).
 */
public class AbstractImplTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "abstractimpl",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar(K, Collection<V>)"});
  }
}
