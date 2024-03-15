package com.example;

import java.util.List;

class Simple {

    public List<Integer> bar(List<SqlNode> nodes) {
        return transform(nodes, SqlNode::getParserPosition);
    }

    public static <F, T> List<T> transform(List<? extends F> list,
                                           java.util.function.Function<? super F, ? extends T> function) {
        throw new Error();
    }
}
