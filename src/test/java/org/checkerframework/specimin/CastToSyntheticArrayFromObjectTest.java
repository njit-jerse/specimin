package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * A cast to an array of a synthetic type, applied to an {@code Object}. Again there is no pair of
 * element types to relate, but here nothing needs to be done: JLS 4.10.3 makes {@code Object} a
 * supertype of every array type, so the cast is already a legal downcast and {@code Baz} is
 * correctly left with no supertype.
 */
public class CastToSyntheticArrayFromObjectTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "casttosyntheticarrayfromobject",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#target(Object)"});
  }
}
