package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * Like {@link AnnotationMethodUsageTest}, but the call to the annotation member is an argument to
 * another call, so JavaParser must compute the member call's type rather than just resolve it. This
 * is the minimized trigger for <a href="https://github.com/njit-jerse/specimin/issues/521">issue
 * 521</a>.
 */
public class AnnotationMethodUsageInArgumentTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "annotationmethodusageinargument",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#foo(Anno)"});
  }
}
