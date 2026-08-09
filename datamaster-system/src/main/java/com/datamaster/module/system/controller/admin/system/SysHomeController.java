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
            @RequestParam(value = "spaceId", required = false) Long spaceId,
            @RequestParam(value = "spaceCode", required = false) String spaceCode) {
        Map<String, Object> data = sysHomeService.getHomeStats(spaceId, spaceCode);
        return AjaxResult.success(data);
    }
}
