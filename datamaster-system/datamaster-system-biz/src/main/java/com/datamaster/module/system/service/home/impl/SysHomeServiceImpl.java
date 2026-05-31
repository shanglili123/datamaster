package com.datamaster.module.system.service.home.impl;

import com.datamaster.module.system.dal.mapper.SysHomeMapper;
import com.datamaster.module.system.service.home.ISysHomeService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SysHomeServiceImpl implements ISysHomeService {

    @Resource
    private SysHomeMapper sysHomeMapper;

    @Override
    public Map<String, Object> getHomeStats(Long projectId, String projectCode) {
        Map<String, Object> data = new HashMap<>();
        data.put("projectId", projectId);
        data.put("projectCode", projectCode);

        if (projectId == null) {
            data.put("integrationTaskTotal", 0);
            data.put("integrationTaskFailed", 0);
            data.put("developTaskTotal", 0);
            data.put("developTaskFailed", 0);
            data.put("apiCallTotal", 0);
            data.put("apiCallFailed", 0);
            data.put("tableRows", Collections.emptyList());
            return data;
        }

        long integrationTaskTotal = sysHomeMapper.countIntegrationTaskTotal(projectId);
        long integrationTaskFailed = sysHomeMapper.countIntegrationTaskFailed(projectId);
        long developTaskTotal = sysHomeMapper.countDevelopTaskTotal(projectId);
        long developTaskFailed = sysHomeMapper.countDevelopTaskFailed(projectId);
        long apiCallTotal = sysHomeMapper.countApiCallTotal();
        long apiCallFailed = sysHomeMapper.countApiCallFailed();
        List<Map<String, Object>> tableRows = sysHomeMapper.selectTableRows(projectId);

        data.put("integrationTaskTotal", integrationTaskTotal);
        data.put("integrationTaskFailed", integrationTaskFailed);
        data.put("developTaskTotal", developTaskTotal);
        data.put("developTaskFailed", developTaskFailed);
        data.put("apiCallTotal", apiCallTotal);
        data.put("apiCallFailed", apiCallFailed);
        data.put("tableRows", tableRows != null ? tableRows : Collections.emptyList());
        return data;
    }
}
