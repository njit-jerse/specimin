package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * This test checks that an overload whose parameter type is a type variable is not treated as
 * definitely applicable to a type-variable argument just because the argument's bound is assignable
 * to it: the parameter's own bound must be respected.
 *
 * <p>{@code clear(X)} is applicable only if inference can choose {@code X} so that {@code Y <: X <:
 * View} (JLS 18.5.1), which needs {@code Y <: View}. {@code Y}'s supertypes are its bound {@code
 * Target} and {@code Target}'s supertypes (JLS 4.10.2), all of which are known and none of which is
 * {@code View}, so {@code clear(X)} is not applicable. javac selects {@code clear(Target)}, and
 * {@code clear(X)} is omitted along with {@code View}.
 */
public class TypeVarParamOverloadTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "typevarparamoverload",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#foo(Y)"});
  }
}
