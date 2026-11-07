package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * This test is the constructor counterpart of {@link UnsolvedAncestorOfArgumentTest}: choosing a
 * constructor for {@code new Holder(this)} requires deciding whether {@code Simple}, which
 * implements an interface that is not on the source path, can be passed to a {@code Listener}
 * parameter (JLS 15.9.3). Derived from issue #562.
 */
public class UnsolvedAncestorOfConstructorArgumentTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "unsolvedancestorofconstructorargument",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar()"});
  }
}
