package com.example;

// This code adapted from the Checker Framework
// (checkerframework.org and github.com/typetools/checker-framework)

import java.util.Comparator;
import java.util.IdentityHashMap;

import org.checkerframework.dataflow.cfg.block.Block;

public abstract class AbstractAnalysis {

    /**
     * A worklist is a priority queue of blocks in which the order is given by depth-first ordering to
     * place non-loop predecessors ahead of successors.
     */
    protected static class Worklist {

        /**
         * Map all blocks in the CFG to their depth-first order.
         */
        protected final IdentityHashMap<Block, Integer> depthFirstOrder = new IdentityHashMap<>();

        /**
         * Comparators to allow priority queue to order blocks by their depth-first order, using by
         * forward analysis.
         */
        public class ForwardDFOComparator implements Comparator<Block> {
            @SuppressWarnings("nullness:unboxing.of.nullable")
            @Override
            public int compare(Block b1, Block b2) {
                return depthFirstOrder.get(b1) - depthFirstOrder.get(b2);
            }
        }
    }
}
