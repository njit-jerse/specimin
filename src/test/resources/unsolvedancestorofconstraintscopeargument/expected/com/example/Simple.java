package com.example;

import android.content.ComponentCallbacks2;
import com.example.sql.SqlNode;
import com.example.sql.SqlParserPos;
import com.example.util.Util;

class Simple implements ComponentCallbacks2, Listener {

  private static Iterable<SqlParserPos> toPos(Iterable<? extends SqlNode> nodes, Simple owner) {
    return Util.transform(nodes, node -> node.combine(owner));
  }
}
