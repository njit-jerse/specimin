package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * Like {@link UnresolvableTypeArgumentTest}, but the type with the unresolvable type argument is a
 * member type. Recovering it has to preserve the enclosing {@code Outer} as well, since JLS 6.5.5.2
 * only lets {@code Inner} be named through it.
 */
public class UnresolvableTypeArgumentInnerClassTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "unresolvabletypeargumentinnerclass",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar()"});
  }
}
