package org.checkerframework.specimin.unsolved;

import com.github.javaparser.ast.Node;
import java.util.Map;
import java.util.Set;

/**
 * Represents a result from the UnsolvedSymbolEnumerator. {@link #classNamesToFileContent()}
 * represents a map of generated symbols' fully qualified names to their file content, and {@link
 * #unusedDependentNodes()} represents Nodes not used in this iteration of UnsolvedSymbolEnumerator,
 * which are safe to remove.
 */
public record UnsolvedSymbolEnumeratorResult(
    Map<String, String> classNamesToFileContent, Set<Node> unusedDependentNodes) {}
