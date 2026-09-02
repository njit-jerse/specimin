package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * Checks that the receiver's type arguments are substituted into a member's declared type (JLS
 * 4.5.2) when the receiver is a field of an anonymous class that sits inside a generic method.
 *
 * <p>An anonymous class cannot declare type parameters of its own (JLS 15.9.5), so every type
 * variable that its members can mention belongs to an enclosing declaration -- here the {@code <U>}
 * of the enclosing method. That is what makes this shape worth pinning: reasoning that walks
 * outwards from a member's declared type looking for the declaration that binds a type variable
 * passes straight through the anonymous class and lands on that enclosing method, which has nothing
 * to do with the member being used.
 */
public class SubstitutedTypeVarInAnonymousClassTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "substitutedtypevarinanonymousclass",
        new String[] {"com/example/Simple.java", "com/example/Container.java"},
        new String[] {"com.example.Simple#bar(Container<Absent>)"});
  }
}
