package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * {@code Impl#apply} has one parameter whose type is not on the source path, so Specimin cannot
 * compute its signature and compares it against {@code Rule#apply} approximately. That comparison
 * erases the type arguments of the other parameter, whose type {@code Outer<String>.Inner<Integer>}
 * carries two type argument lists: one for the enclosing type and one for the member type.
 *
 * <p>The purpose of this test is to rule out an incorrect implementation of erasure in that comparison
 * that greedily deletes everything between angle brackets (which Specimin used to have).
 */
public class QualifiedInnerTypeErasureTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "qualifiedinnertypeerasure",
        new String[] {"com/example/Target.java"},
        new String[] {"com.example.Target#target(Outer<String>.Inner<Integer>, Missing)"});
  }
}
