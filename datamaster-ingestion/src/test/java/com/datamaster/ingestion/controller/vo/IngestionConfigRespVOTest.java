package com.datamaster.ingestion.controller.vo;

import com.datamaster.ingestion.config.IngestionProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class IngestionConfigRespVOTest {

    @Test
    public void shouldExposeConfigurationSummaryWithoutSecret() throws Exception {
        IngestionProperties properties = new IngestionProperties();
        properties.setKeyColumns(Arrays.asList("id"));
        properties.getDecisionTrigger().setEnabled(true);
        properties.getDecisionTrigger().setBaseUrl("http://ontology");
        properties.getDecisionTrigger().setSecret("do-not-expose");

        IngestionConfigRespVO result = IngestionConfigRespVO.from(properties);

        assertEquals(Arrays.asList("id"), result.getKeyColumns());
        assertEquals("http://ontology", result.getDecisionTriggerBaseUrl());
        String json = new ObjectMapper().writeValueAsString(result);
        assertFalse("触发密钥不能通过配置接口返回", json.contains("do-not-expose"));
    }
}
