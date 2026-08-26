package org.apache.commons.lang3.builder;

public class ToStringBuilder extends AbstractReflection implements Builder<String> {

  private final StringBuffer buffer = null;

  private final ToStringStyle style = null;

  public ToStringBuilder append(final int[] array) {
    style.append(buffer, null, array, null);
    return this;
  }
}
