package com.example;

import java.util.List;

class Simple {
    // Target method.
    public List<Integer> bar(List<SqlNode> nodes) {
        return transform(nodes, SqlNode::getParserPosition);
    }


    // From Apache Calcite
    /** Transforms a list, applying a function to each element. */
    public static <F, T> List<T> transform(List<? extends F> list,
                                           java.util.function.Function<? super F, ? extends T> function) {
        throw new java.lang.Error();
    }
}
