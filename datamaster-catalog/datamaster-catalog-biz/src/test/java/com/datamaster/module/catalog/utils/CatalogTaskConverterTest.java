package com.datamaster.module.catalog.utils;

import com.datamaster.api.ds.api.etl.DsTaskSaveReqDTO;
import com.datamaster.module.catalog.utils.model.TaskSaveReqInput;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CatalogTaskConverterTest {

    @Test
    public void buildDsTaskSaveReqShouldUseDs342SupportedHttpParameterType() {
        TaskSaveReqInput input = new TaskSaveReqInput();
        input.setName("metadata-collect");
        input.setId(10001L);
        input.setNodeCode("20002");
        input.addHttpParam("id", "PARAMETER", "10001");

        DsTaskSaveReqDTO reqDTO = CatalogTaskConverter.buildDsTaskSaveReq(input);

        assertTrue(reqDTO.getTaskDefinitionJson().contains("\"httpParametersType\":\"PARAMETER\""));
        assertFalse(reqDTO.getTaskDefinitionJson().contains("\"httpParametersType\":\"BODY\""));
    }
}
