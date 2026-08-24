package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * Like {@link CastToSyntheticArrayTest}, but two-dimensional: both bracket levels have to come off
 * before the supertype is recorded, since JLS 4.10.3 applies once per level.
 */
public class CastToSyntheticArray2dTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "casttosyntheticarray2d",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#target(Bar[][])"});
  }
}
