package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * Two use sites constrain the result of the same unsolved method: one reads a member off it, and
 * one assigns it to a variable of a known type. JLS 15.12.1 requires the return type to have a
 * member named {@code foo}, and JLS 5.2 makes the initializer of {@code Payload p} an assignment
 * context, so the return type must also be assignable to {@code Payload}. A synthetic subtype of
 * {@code Payload} that declares {@code foo} satisfies both; reporting {@code Payload} flatly
 * satisfies only the second, and does not compile.
 *
 * <p>The point of the test is that this does not depend on which use site Specimin reaches first.
 * {@link ContextTypeThenMemberAccessTest} is this same program with the two statements swapped, and
 * its expected output is identical apart from that swap.
 */
public class MemberAccessThenContextTypeTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "memberaccessthencontexttype",
        new String[] {"com/example/Simple.java", "com/example/Payload.java"},
        new String[] {"com.example.Simple#bar(Item)"});
  }
}
