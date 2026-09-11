package com.example;

import com.example.sql.SqlNode;
import com.example.sql.SqlParserPos;
import com.example.util.Util;

class Simple {

  private static Iterable<SqlParserPos> toPos(
      Iterable<? extends SqlNode> nodes, Iterable<? extends SqlNode> others) {
    return Util.transform2(nodes, others, (node, other) -> node.combine(other));
  }
}
