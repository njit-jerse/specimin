package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * Checks that a cast to a primitive type constrains the type inferred for the cast's operand. This
 * covers the remaining way a cast target can fail to be a reference type, alongside {@link
 * CastToSyntheticTest} (a reference type that does not resolve). {@code Object} is the right answer
 * because {@code (int) someObject} is legal: it is a narrowing reference conversion to {@code
 * Integer} followed by unboxing. A freshly generated synthetic return type would not compile here.
 */
public class CastToPrimitiveTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "casttoprimitive",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#target(Foo)"});
  }
}
