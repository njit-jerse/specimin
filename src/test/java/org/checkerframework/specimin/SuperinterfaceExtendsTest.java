package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks that we properly preserve superinterface bounds. Based on the bug JDK-8319461.
 */
public class SuperinterfaceExtendsTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "superinterfaceextends",
        new String[] {"com/example/PropertyFactoryManager.java"},
        new String[] {"com.example.PropertyFactoryManager#create(Class<P>, Class<V>)"});
  }
}
