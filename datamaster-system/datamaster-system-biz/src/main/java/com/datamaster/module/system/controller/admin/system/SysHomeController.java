package com.datamaster.module.system.controller.admin.system;

import com.datamaster.common.core.domain.AjaxResult;
import com.datamaster.module.system.service.home.ISysHomeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Map;

@RestController
public class SysHomeController {

    @Resource
    private ISysHomeService sysHomeService;

    @GetMapping("/home")
    public AjaxResult home(
            @RequestParam(value = "projectId", required = false) Long projectId,
            @RequestParam(value = "projectCode", required = false) String projectCode) {
        Map<String, Object> data = sysHomeService.getHomeStats(projectId, projectCode);
        return AjaxResult.success(data);
    }
}
