package com.example;

import java.util.Comparator;
import java.util.IdentityHashMap;
import org.checkerframework.dataflow.cfg.block.Block;

public abstract class AbstractAnalysis {

    protected static class Worklist {

        protected final IdentityHashMap<Block, Integer> depthFirstOrder = null;

        public class ForwardDFOComparator implements Comparator<Block> {

            @SuppressWarnings("nullness:unboxing.of.nullable")
                        public int compare(Block b1, Block b2) {
                return depthFirstOrder.get(b1) - depthFirstOrder.get(b2);
            }
        }
    }
}
