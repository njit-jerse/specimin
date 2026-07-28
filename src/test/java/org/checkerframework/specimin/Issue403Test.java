package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * Regression test case for <a href="https://github.com/njit-jerse/specimin/issues/403">issue
 * 403</a>.
 */
public class Issue403Test {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "issue403",
        new String[] {"com/example/BasicDeserializerFactory.java"},
        new String[] {
          "com.example.BasicDeserializerFactory#createArrayDeserializer(DeserializationContext,"
              + " ArrayType, BeanDescription)"
        });
  }
}
