package org.checkerframework.specimin;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * This test checks that TargetMethodFinderVisitor can correctly find the target method when there
 * is an annotation in a generic type in the original source. This is a regression test.
 */
public class AnnoInGenericTargetTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "annoingenerictarget",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#unmodifiableCollection(Collection<? extends T>)"});
  }

  @Test
  public void unitTestRemoveMethodReturnTypeAndAnnotations() {
    assertEquals(
        "unmodifiableCollection(Collection<? extends T>)",
        JavaParserUtil.removeMethodReturnTypeAndAnnotationsImpl(
            "Collection<T> unmodifiableCollection(Collection<@Initialized @KeyForBottom"
                + " @NonNull ? extends T>)"),
        "TargetMethodFinderVisitor#removeMethodReturnTypeAndAnnotations is failing to properly"
            + " remove some annotations");
  }
}
