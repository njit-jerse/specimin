package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * {@link MemberAccessThenContextTypeTest} with the two statements swapped. See that test for what
 * the two use sites require; the expected outputs of the two tests differ only in the order of the
 * statements in {@code Simple.java}, which is the property being checked.
 */
public class ContextTypeThenMemberAccessTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "contexttypethenmemberaccess",
        new String[] {"com/example/Simple.java", "com/example/Payload.java"},
        new String[] {"com.example.Simple#bar(Item)"});
  }
}
