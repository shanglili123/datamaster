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
    public Map<String, Object> getHomeStats(Long spaceId, String spaceCode) {
        Map<String, Object> data = new HashMap<>();
        data.put("spaceId", spaceId);
        data.put("spaceCode", spaceCode);

        if (spaceId == null) {
            data.put("integrationTaskTotal", 0);
            data.put("integrationTaskFailed", 0);
            data.put("developTaskTotal", 0);
            data.put("developTaskFailed", 0);
            data.put("apiCallTotal", 0);
            data.put("apiCallFailed", 0);
            data.put("tableRows", Collections.emptyList());
            return data;
        }

        long integrationTaskTotal = sysHomeMapper.countIntegrationTaskTotal(spaceId);
        long integrationTaskFailed = sysHomeMapper.countIntegrationTaskFailed(spaceId);
        long developTaskTotal = sysHomeMapper.countDevelopTaskTotal(spaceId);
        long developTaskFailed = sysHomeMapper.countDevelopTaskFailed(spaceId);
        long apiCallTotal = sysHomeMapper.countApiCallTotal(spaceId);
        long apiCallFailed = sysHomeMapper.countApiCallFailed(spaceId);
        long datasourceTotal = sysHomeMapper.countDatasourceTotal(spaceId);
        long catalogTableTotal = sysHomeMapper.countCatalogTableTotal(spaceId);
        long apiTotal = sysHomeMapper.countApiTotal(spaceId);
        long dataElemTotal = sysHomeMapper.countDataElemTotal(spaceId);
        long modelTotal = sysHomeMapper.countModelTotal(spaceId);
        long tagTotal = sysHomeMapper.countTagTotal(spaceId);
        long collectTaskTotal = sysHomeMapper.countCollectTaskTotal(spaceId);
        long documentTotal = sysHomeMapper.countDocumentTotal(spaceId);
        List<Map<String, Object>> tableRows = sysHomeMapper.selectTableRows(spaceId);

        data.put("integrationTaskTotal", integrationTaskTotal);
        data.put("integrationTaskFailed", integrationTaskFailed);
        data.put("developTaskTotal", developTaskTotal);
        data.put("developTaskFailed", developTaskFailed);
        data.put("apiCallTotal", apiCallTotal);
        data.put("apiCallFailed", apiCallFailed);
        data.put("datasourceTotal", datasourceTotal);
        data.put("catalogTableTotal", catalogTableTotal);
        data.put("apiTotal", apiTotal);
        data.put("dataElemTotal", dataElemTotal);
        data.put("modelTotal", modelTotal);
        data.put("tagTotal", tagTotal);
        data.put("collectTaskTotal", collectTaskTotal);
        data.put("documentTotal", documentTotal);
        data.put("tableRows", tableRows != null ? tableRows : Collections.emptyList());
        return data;
    }
}
