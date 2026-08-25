package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * This test checks where Specimin puts the package/type boundary when both the class name and the
 * field name are acronyms. A package named {@code org.apache.commons.io.IOUtils} is not a package
 * that Java code would have, so {@code IOUtils} names the type and {@code FIELD} is a static field
 * of it.
 */
public class AcronymClassStaticFieldCallTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "acronymclassstaticfieldcall",
        new String[] {"com/example/Foo.java"},
        new String[] {"com.example.Foo#bar()"});
  }
}
