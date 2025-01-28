src/test/java/org/checkerframework/specimin/NoZeroArgCtorTest.javapackage org.checkerframework.specimin;

import org.junit.Test;

import java.io.IOException;

/**
 * This test checks that when extending a non-JDK class with only constructors that take more than zero
 * arguments, at least one constructor gets preserved so that the result compiles.
 */
public class NoZeroArgCtorTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "nozeroargctor",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar()"});
  }
}
