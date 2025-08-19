package com.example;

import static com.example.nullness.Nullness.castNonNull;

import com.example.sql.SqlNode;
import com.example.sql.SqlParserPos;
import com.example.util.Util;

class Simple {

    private static Iterable<SqlParserPos> toPos(Iterable<? extends SqlNode> nodes) {
        return Util.transform(
            nodes, node -> node == null ? castNonNull(null) : node.getParserPosition());
    }
}
