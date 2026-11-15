package com.datamaster.module.ai.controller.admin.profile;

import com.datamaster.module.ai.controller.admin.profile.vo.AiModelRoutingSaveReqVO;
import org.junit.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import javax.servlet.http.HttpServletRequest;
import static org.junit.Assert.*;

public class AiModelRoutingControllerTest {
    @Test public void readAndSaveReuseExistingModelManagementPermissions() throws Exception {
        assertTrue(AiModelRoutingController.class.getMethod("get", Long.class, String.class, HttpServletRequest.class)
                .getAnnotation(PreAuthorize.class).value().contains("ai:model-config:list"));
        assertTrue(AiModelRoutingController.class.getMethod("save", AiModelRoutingSaveReqVO.class, HttpServletRequest.class)
                .getAnnotation(PreAuthorize.class).value().contains("ai:model-config:edit"));
    }
}
