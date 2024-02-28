package org.checkerframework.specimin;

import com.google.common.base.Equivalence;

/**
 * A class to check strict equivalence relation for instances of UnsolvedClassOrInterface. The
 * comparison is based on their qualified class names and set of methods.
 */
public class ClassStrictEquivalence extends Equivalence<UnsolvedClassOrInterface> {

  @Override
  protected boolean doEquivalent(
      UnsolvedClassOrInterface thisClass, UnsolvedClassOrInterface otherClass) {
    return otherClass.getQualifiedClassName().equals(thisClass.getQualifiedClassName())
        && otherClass.getMethods().equals(thisClass.getMethods());
  }

  @Override
  protected int doHash(UnsolvedClassOrInterface someClass) {
    return someClass.getQualifiedClassName().hashCode() + someClass.getMethods().hashCode();
  }
}
