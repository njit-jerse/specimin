package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * This test checks that a field whose initializer mentions a type Specimin cannot resolve does not
 * crash Specimin.
 *
 * <p>Specimin deliberately discards field initializers: no typing judgment about a target member
 * reads one (JLS 8.3 gives the expression {@code defaults} the field's <em>declared</em> type), so
 * the initializer's own types never need to be preserved. Accordingly {@code
 * androidx.collection.ArrayMap} is absent from the expected output, and the final field is given
 * the default initializer that JLS 16.8 definite assignment requires.
 */
public class UnsolvedFieldInitializerTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "unsolvedfieldinitializer",
        new String[] {"com/example/Foo.java"},
        new String[] {"com.example.Foo#build(Unsolved)"});
  }
}
