package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * Like {@link UnresolvableTypeArgumentTest}, but the parameterized type appears as the target
 * method's parameter type rather than in an implements clause: losing an in-project type because
 * one of its type arguments is unresolvable is not specific to supertypes.
 */
public class UnresolvableTypeArgumentInParamTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "unresolvabletypeargumentinparam",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar(Container<Absent>)"});
  }
}
