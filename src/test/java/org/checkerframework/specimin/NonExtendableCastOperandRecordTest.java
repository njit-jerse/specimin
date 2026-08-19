package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * The record counterpart of {@link NonExtendableCastOperandPrimitiveTest}, which previously emitted
 * a class extending the record. A record is implicitly final (JLS 8.10), and unlike a primitive or
 * a final JDK class it can only be recognized by looking at its declaration, so this covers the
 * path through {@code JavaParserUtil#isNonExtendableTypeName} that consults the project's
 * compilation units.
 *
 * <p>The record is declared with no components so that the test turns only on the declaration's
 * kind; see {@link NonExtendableTargetEnumTest}.
 */
public class NonExtendableCastOperandRecordTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "nonextendablecastoperandrecord",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#target(Foo)"});
  }
}
