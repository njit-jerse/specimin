package org.anonymous.specslice;

import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

/**
 * A specialized version of Java Parser's ReflectionTypeSolver that allows access to compiler
 * internals (e.g., com.sun and jdk. packages) in addition to the JRE, but does not include anything
 * else on SpecSlice's classpath (especially JavaParser itself).
 */
public class JdkTypeSolver extends ReflectionTypeSolver {

  @Override
  protected boolean filterName(String name) {
    return JavaLangUtils.inJdkPackage(name);
  }
}
