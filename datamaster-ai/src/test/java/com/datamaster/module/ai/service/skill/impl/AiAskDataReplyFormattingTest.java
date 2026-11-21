package com.datamaster.module.ai.service.skill.impl;

import org.junit.Test;
import java.lang.reflect.Method;
import static org.junit.Assert.*;

public class AiAskDataReplyFormattingTest {
    @Test public void traceCleanupPreservesRealNewlinesAndMarkdownTableBoundaries() throws Exception {
        String input = "查询结果：\n\n| 名称 | 数量 |\n| --- | --- |\n| sales_order | 6 |\n\n数据已核对。";
        String output = normalize(input);
        assertTrue(output.contains("\n| 名称 | 数量 |\n| --- | --- |\n| sales_order | 6 |\n"));
        assertFalse(output.contains("n|"));
        assertTrue(output.endsWith("数据已核对。"));
    }

    @Test public void traceRemovalDoesNotFlattenRemainingTable() throws Exception {
        String input = "思考：内部轨迹\n\n| 名称 | 数量 |\n| --- | --- |\n| product | 4 |";
        String output = normalize(input);
        assertFalse(output.contains("内部轨迹"));
        assertTrue(output.contains("| 名称 | 数量 |\n| --- | --- |\n| product | 4 |"));
    }

    @Test public void removesLeadingHeaderlessPreviewGluedToDescription() throws Exception {
        String input = "| --- |\n| 4 |当前共有 4 个产品。\n\n| product_count |\n| --- |\n| 4 |";
        assertEquals("当前共有 4 个产品。\n\n| product_count |\n| --- |\n| 4 |", normalize(input));
    }

    @Test public void removesLeadingHeaderlessPreviewGluedToAnotherTable() throws Exception {
        String input = "| --- |\n| 25 || order_count |\n| --- |\n| 25 |";
        assertEquals("| order_count |\n| --- |\n| 25 |", normalize(input));
    }

    @Test public void keepsPureHeaderlessTable() throws Exception {
        String input = "| --- |\n| 4 |";
        assertEquals(input, normalize(input));
    }

    @Test public void keepsMultiColumnHeaderlessTable() throws Exception {
        String input = "| --- | --- |\n| sales_order | 6 |\n| product | 4 |";
        assertEquals(input, normalize(input));
    }

    private String normalize(String text) throws Exception {
        Method method = AiAskDataServiceImpl.class.getDeclaredMethod("normalizeAskReply", String.class);
        method.setAccessible(true);
        return (String) method.invoke(new AiAskDataServiceImpl(), text);
    }
}
