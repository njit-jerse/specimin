package com.example;

import com.example.sql.SqlNode;
import com.example.sql.SqlParserPos;
import com.example.util.Util;

class Simple {
    // Target method.
    private static Iterable<SqlParserPos> toPos(
            Iterable<? extends SqlNode> nodes) {
        return Util.transform(nodes, node -> node.getParserPosition("x"));
    }
}
