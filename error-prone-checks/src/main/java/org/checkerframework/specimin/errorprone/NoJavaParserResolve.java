package org.checkerframework.specimin.errorprone;

import com.google.auto.service.AutoService;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MemberReferenceTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MethodInvocationTree;

import static com.google.errorprone.matchers.Matchers.instanceMethod;

@AutoService(BugChecker.class)
@BugPattern(
        name = "NoJavaParserResolve",
        summary = "Do not call JavaParser's Resolvable#resolve() directly; use " +
                "Resolver#resolve(Resolvable<T>) or Resolver#resolveGuaranteeNonNull(Resolvable<T>) " +
                "instead, which handle known JavaParser resolution bugs.",
        severity = BugPattern.SeverityLevel.ERROR,
        linkType = BugPattern.LinkType.NONE
)
public final class NoJavaParserResolve extends BugChecker
        implements MethodInvocationTreeMatcher, MemberReferenceTreeMatcher {

    private static final Matcher<ExpressionTree> RESOLVE_MATCHER =
            instanceMethod()
                    .onDescendantOf("com.github.javaparser.resolution.Resolvable")
                    .named("resolve")
                    .withNoParameters();

    @Override
    public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
        return match(tree, state);
    }

    @Override
    public Description matchMemberReference(MemberReferenceTree tree, VisitorState state) {
        return match(tree, state);
    }

    private Description match(ExpressionTree tree, VisitorState state) {
        if (!RESOLVE_MATCHER.matches(tree, state)) {
            return Description.NO_MATCH;
        }
        return buildDescription(tree)
                .setMessage(
                        "Use Resolver#resolve(Resolvable<T>) instead of Resolvable#resolve() to properly " +
                                "handle known JavaParser bugs relating to resolution.")
                .build();
    }
}