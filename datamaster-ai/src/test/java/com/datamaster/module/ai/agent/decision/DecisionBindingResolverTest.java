package com.datamaster.module.ai.agent.decision;

import com.datamaster.common.exception.ServiceException;
import org.junit.Test;

import static org.junit.Assert.*;

public class DecisionBindingResolverTest {
    @Test
    public void canonicalDigestDoesNotDependOnObjectKeyOrder() {
        String first = DecisionBindingResolver.canonical(DecisionBindingResolver.parseInput("{\"b\":2,\"a\":1}"));
        String second = DecisionBindingResolver.canonical(DecisionBindingResolver.parseInput("{\"a\":1,\"b\":2}"));
        assertEquals(first, second);
        assertEquals(DecisionBindingResolver.sha256(first), DecisionBindingResolver.sha256(second));
    }

    @Test(expected = ServiceException.class)
    public void arraysAreNotExecutionParameters() {
        DecisionBindingResolver.parseInput("[]");
    }

    @Test(expected = ServiceException.class)
    public void negativeTargetIdIsRejected() {
        DecisionBindingResolver.positiveId("-1");
    }
}
