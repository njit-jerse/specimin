package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks if Specimin properly preserves a method in an interface that actually gets used
 * by a field of that interface's type.
 */
public class InterfaceUseComplexTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "interfaceusecomplex",
        new String[] {"com/example/Foo.java"},
        new String[] {"com.example.Foo.Qux#containsAll(Baz<?>)"});
  }
}
