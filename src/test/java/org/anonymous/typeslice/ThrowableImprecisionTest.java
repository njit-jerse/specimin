package org.anonymous.typeslice;

import java.io.IOException;
import org.junit.Test;

/**
 * This test demonstrates the imprecision of TypeSlice when there are two or more synthetic classes
 * with the same name. If only one or some of them extend Throwable, TypeSlice will imprecisely make
 * all of them extend Throwable. This occurs due to TypeSlice becoming confused when faced with
 * multiple synthetic classes sharing the same name. This imprecision is caused by the fact that
 * TypeSlice uses javac's error messages to determine which synthetic types need to extend
 * Throwable, but javac only prints simple names in its error messages.
 */
public class ThrowableImprecisionTest {
  @Test
  public void runTest() throws IOException {
    TypeSliceTestExecutor.runTestWithoutJarPaths(
        "ThrowableImprecision",
        new String[] {"com/example/Foo.java", "com/example/Baz.java"},
        new String[] {"com.example.Foo#bar()", "com.example.Baz#bar()"});
  }
}
