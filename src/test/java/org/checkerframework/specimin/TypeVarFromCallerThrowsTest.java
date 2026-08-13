package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * The same leak as {@link StaticTypeVarFromCallerTest}, on a method that also acquires a {@code
 * throws} clause, and with a calling type variable whose name is not the one Specimin would have
 * generated.
 *
 * <p>Learning that the method throws replaces its alternates with copies. The goal of this test
 * case is to ensure that these copies are faithful. The name of the type variable in the input
 * therefore has to differ from Specimin's default generated name: with a calling type variable
 * named {@code T}, as in the other fixtures in this group, the reconstruction would only happen
 * to agree.
 */
public class TypeVarFromCallerThrowsTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "typevarfromcallerthrows",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#target(Src<V>)"});
  }
}
