package com.example;

import com.example.sql.SqlNode;
import com.example.sql.SqlParserPos;
import com.example.util.Util;

class Simple {
    private static <T> Iterable<SqlParserPos> toPos(
            Iterable<? extends SqlNode> nodes, T t) {
        return Util.transform(nodes, node -> node.combine(t));
    }
}
