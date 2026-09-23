package com.example;

import org.example.Ctx;

class Simple {
  boolean mirror(Ctx context) {
    return null != context.getChild();
  }

  boolean field(Ctx context) {
    return context.parent == null;
  }

  String conditional(Ctx context, boolean flag) {
    return flag ? context.getSibling() : null;
  }

  int primitiveConditional(Ctx context, boolean flag) {
    int count = flag ? context.getCount() : null;
    return count;
  }

  boolean memberAfterComparison(Ctx context) {
    if (context.getOwner() == null) {
      return false;
    }
    return context.getOwner().isActive();
  }
}
