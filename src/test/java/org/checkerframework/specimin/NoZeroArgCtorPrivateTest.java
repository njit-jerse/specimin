package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * This test checks that when a preserved superclass constructor is private, Specimin does not use
 * it as the target of the super(...) call in the constructor that it preserves in the subclass,
 * even if it is the constructor with the fewest parameters. A subclass cannot call a private
 * constructor, so doing so would produce output that does not compile.
 */
public class NoZeroArgCtorPrivateTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "nozeroargctorprivate",
        new String[] {"com/example/Simple.java", "com/example/SomeOtherClass.java"},
        new String[] {"com.example.Simple#bar()", "com.example.SomeOtherClass#make()"});
  }
}
