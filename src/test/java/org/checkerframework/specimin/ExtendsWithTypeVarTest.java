 package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks that classes that have type vars and extends a type with type vars do not create
 * extraneous classes, based on an example from Guava.
 */
public class ExtendsWithTypeVarTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "extendswithtypevar",
        new String[] {"com/example/AbstractMapEntry.java", "com/example/Simple.java"},
        new String[] {"com.example.Simple#bar(AbstractMapEntry<K, V>)"});
  }
}
