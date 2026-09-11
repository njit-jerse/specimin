package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * Regression test adapted from greenrobot/EventBus's {@code AndroidComponents}. The target field,
 * {@code implementation}, is a {@code static final} field with no declaration-site initializer
 * that is instead assigned in a static initializer block, conditionally, based on a call to
 * another class in the same package. This exercises following a static initializer block as a
 * dependency instead of letting Specimin's "empty final field" repair invent a default value.
 */
public class AndroidComponentsStaticBlockTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "androidcomponentsstaticblock",
        new String[] {
          "org/greenrobot/eventbus/android/AndroidComponents.java",
          "org/greenrobot/eventbus/android/AndroidDependenciesDetector.java"
        },
        new String[] {"org.greenrobot.eventbus.android.AndroidComponents#implementation"});
  }
}
