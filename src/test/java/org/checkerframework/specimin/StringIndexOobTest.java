package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

public class StringIndexOobTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "stringindexoob",
        new String[] {"com/example/BasicDeserializerFactory.java"},
        new String[] {
          "com.example.BasicDeserializerFactory#createArrayDeserializer(DeserializationContext,"
              + " ArrayType, BeanDescription)"
        });
  }
}
