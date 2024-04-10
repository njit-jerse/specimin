package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Assert;
import org.junit.Test;

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
    Assert.assertEquals(
        "TargetMethodFinderVisitor#removeMethodReturnTypeAndAnnotations is failing to properly"
            + " remove some annotations",
        "unmodifiableCollection(Collection<? extends T>)",
        TargetMethodFinderVisitor.removeMethodReturnTypeAndAnnotations(
            "Collection<T> unmodifiableCollection(Collection<@Initialized @KeyForBottom @NonNull ?"
                + " extends T>)"));
  }
}
