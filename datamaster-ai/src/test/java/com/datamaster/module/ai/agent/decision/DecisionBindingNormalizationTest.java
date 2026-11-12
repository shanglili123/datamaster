package com.datamaster.module.ai.agent.decision;

import com.datamaster.module.ai.agent.decision.DecisionModels.Binding;
import org.junit.Test;
import static org.junit.Assert.*;

public class DecisionBindingNormalizationTest {
    @Test public void inheritsAndTrimsObjectWithoutMutatingOriginal() {
        Binding original = new Binding(); original.setType("composite"); original.setTargetId("9");
        original.setObjectKey(" "); original.setInputParams("{\"z\":1,\"a\":2}");
        Binding effective = DecisionBindingNormalization.effective(original, " ORD-1 ");
        assertEquals("ACTION", effective.getType()); assertEquals("ORD-1", effective.getObjectKey());
        assertEquals("{\"a\":2,\"z\":1}", effective.getInputParams());
        assertEquals(" ", original.getObjectKey()); assertEquals("composite", original.getType());
    }

    @Test public void explicitObjectWinsAndMissingObjectRemainsNull() {
        Binding original = new Binding(); original.setType("ACTION"); original.setObjectKey(" ORD-2 ");
        assertEquals("ORD-2", DecisionBindingNormalization.effective(original, "ORD-1").getObjectKey());
        original.setObjectKey(null);
        assertNull(DecisionBindingNormalization.effective(original, null).getObjectKey());
        assertNull(DecisionBindingNormalization.effective(null, "ORD-1"));
    }
}
