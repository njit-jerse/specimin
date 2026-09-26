package org.checkerframework.specimin.modularity;

/** The modularity model for OpenJML. TODO: should only enable output files to preserve comments. */
public class OpenJMLModularityModel implements ModularityModel {
  @Override
  public boolean preserveAllComments() {
    return true;
  }
}
