package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks that an imported declaration annotation is retained. Based on a bug I discovered
 * in CF-6030.
 */
public class ImportAnnoTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTest(
        "importanno",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar()"},
        "cf",
        new String[] {"src/test/resources/shared/checker-qual-3.42.0.jar"});
  }
}
