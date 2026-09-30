package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * This test checks that a {@code super.m(...)} call that JavaParser cannot resolve keeps the
 * concrete {@code m} in the direct superclass, even though a generic superinterface of that class
 * declares an abstract {@code m} that the call could also match. Keeping only the interface method
 * would not compile: JLS 15.12.3 forbids {@code super.m} from invoking an abstract method. This is
 * the shape of the program in issue 558.
 */
public class SuperMethodInAbstractSuperclassImplementingGenericInterfaceTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "supermethodinabstractsuperclassimplementinggenericinterface",
        new String[] {"com/example/Registry.java"},
        new String[] {"com.example.Registry#register(Info)"});
  }
}
