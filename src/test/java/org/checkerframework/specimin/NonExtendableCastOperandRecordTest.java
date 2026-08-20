package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/** The record counterpart of {@link NonExtendableCastOperandPrimitiveTest}. */
public class NonExtendableCastOperandRecordTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "nonextendablecastoperandrecord",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#target(Foo)"});
  }
}
