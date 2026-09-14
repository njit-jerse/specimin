package com.example.sql;

import com.example.other.Unrelated;

public class SqlNode {
    // Not applicable to combine(node): Unrelated is unrelated to SqlNode.
    public SqlOther combine(Unrelated u) {
        throw new java.lang.Error();
    }

    public SqlParserPos combine(SqlNode n) {
        throw new java.lang.Error();
    }
}
