package org.example;

import external.Nullable;

public final class ClassInfo {

  @Nullable("for inner classes")
  private String pkg;

  @Nullable private ClassInfo parentClass;

  public String getPackage() {
    if (parentClass != null) {
      return parentClass.getPackage();
    }
    if (pkg == null) {
      throw new RuntimeException("Package is null for not inner class");
    }
    return pkg;
  }
}
