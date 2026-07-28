package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/** This test checks if Specimin will work for record types */
public class RecordsTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "records", new String[] {"com/example/Foo.java"}, new String[] {"com.example.Foo#foo()"});
  }
}
