package org.checkerframework.specimin.modularity;

/** The modularity model for NullAway. */
public class NullAwayModularityModel implements ModularityModel {
  @Override
  public boolean preserveAllFieldsIfTargetIsConstructor() {
    return true;
  }
}
