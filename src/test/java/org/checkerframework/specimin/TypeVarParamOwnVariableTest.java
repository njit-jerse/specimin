package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * This test checks that Specimin preserves an overload whose parameter type is a type variable when
 * the argument is of that same type variable, even though other overloads are declared first.
 *
 * <p>{@code T} has no declared bound, so its only supertype is {@code Object} (JLS 4.10.2). An
 * argument of type {@code T} is therefore not assignable to {@code String} or {@code View}, and
 * only {@code put(T)} is applicable (JLS 15.12.2.2). javac selects it, and the other overloads are
 * omitted along with {@code View}. {@code put(View)} is in the input because its unsolvable
 * parameter type makes JavaParser's own overload resolution fail, which is the path this test
 * exercises.
 */
public class TypeVarParamOwnVariableTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "typevarparamownvariable",
        new String[] {"com/example/Box.java"},
        new String[] {"com.example.Box#use(T)"});
  }
}
