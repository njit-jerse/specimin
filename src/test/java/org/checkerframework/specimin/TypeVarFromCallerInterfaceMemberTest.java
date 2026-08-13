package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * The implicitly static case: a member class of an interface.
 *
 * <p>{@code Helper} carries no {@code static} modifier, but a member type of an interface is
 * implicitly static (JLS 9.1.1.3), so {@code Simple}'s {@code C} is not nameable inside it. That
 * distinction is invisible to {@code isStatic()}, which reports only the written modifier, so this
 * fixture covers the branch that has to recognise it. As with {@link
 * TypeVarFromCallerStaticScopeTest}, what it can actually catch is dropping {@code target}'s own
 * {@code T}.
 */
public class TypeVarFromCallerInterfaceMemberTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "typevarfromcallerinterfacemember",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple.Helper#target(Src<T>)"});
  }
}
