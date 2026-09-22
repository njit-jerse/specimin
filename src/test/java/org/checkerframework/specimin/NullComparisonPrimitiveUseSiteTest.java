package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * This test checks that a comparison with {@code null} still forces a reference type when another
 * use site suggests a primitive. The input compiles only if {@code getParent()} returns a reference
 * type (JLS 15.21 forbids {@code int == null}), so reporting {@code int} would not compile. A
 * generic return type satisfies both sites: {@code T} is inferred as {@code Integer} and unboxed
 * for the {@code int} declaration (JLS 5.2), and any {@code T} can be compared with {@code null}
 * (JLS 15.21.3).
 */
public class NullComparisonPrimitiveUseSiteTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "nullcomparisonprimitiveusesite",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar(Ctx)"});
  }
}
