package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/** This test checks if Specimin is able to process Java builtin enum classes. Issue #361 */
public class BuiltinEnumTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "enumbuiltin",
        new String[] {"com/example/Scheduler.java"},
        new String[] {"com.example.Scheduler#schedule(Runnable, int)"});
  }
}
