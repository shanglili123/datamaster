package com.datamaster.module.ai.service.dbgpt.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.datamaster.common.core.domain.AjaxResult;
import com.datamaster.common.database.utils.AesEncryptUtil;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.common.core.domain.entity.DatasourceDO;
import com.datamaster.common.datasource.mgmt.service.IDatasourceMgmtService;
import com.datamaster.module.ai.model.dto.dbgpt.DbGptDatasourceCreateRequest;
import com.datamaster.module.ai.service.dbgpt.IDbGptClientService;
import com.datamaster.module.assets.service.dbgpt.IDbGptDatasourceSyncService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DbGptDatasourceSyncServiceImpl implements IDbGptDatasourceSyncService {

    @Resource
    private IDatasourceMgmtService datasourceMgmtService;
    @Resource
    private IDbGptClientService dbGptClientService;

    @Override
    public AjaxResult syncAll() {
        int success = 0;
        int failed = 0;
        List<DatasourceDO> datasources = datasourceMgmtService.getDatasourceList();
        for (DatasourceDO datasource : datasources) {
            try {
                syncDatasource(datasource);
                success++;
            } catch (Exception e) {
                failed++;
                markFailed(datasource, e.getMessage());
            }
        }
        return AjaxResult.success("数据智能体数据源同步完成，成功 " + success + " 个，失败 " + failed + " 个");
    }

    @Override
    public AjaxResult syncById(Long datasourceId) {
        DatasourceDO datasource = datasourceMgmtService.getDatasourceDOById(datasourceId);
        if (datasource == null) {
            throw new ServiceException("数据源不存在");
        }
        Integer dbgptId = syncDatasource(datasource);
        return AjaxResult.success("数据智能体数据源同步成功，编号：" + dbgptId);
    }

    @Override
    public AjaxResult removeFromDbGpt(Long datasourceId) {
        DatasourceDO datasource = datasourceMgmtService.getDatasourceDOById(datasourceId);
        if (datasource == null) {
            throw new ServiceException("数据源不存在");
        }
        if (datasource.getDbgptDatasourceId() != null) {
            dbGptClientService.deleteDatasource(datasource.getDbgptDatasourceId().intValue());
        }
        datasource.setDbgptDatasourceId(null);
        datasource.setDbgptSyncStatus("REMOVED");
            datasource.setDbgptSyncMessage("已从数据智能体移除");
        datasourceMgmtService.updateDatasource(datasource);
        return AjaxResult.success("已从数据智能体移除数据源");
    }

    private Integer syncDatasource(DatasourceDO datasource) {
        DbGptDatasourceCreateRequest request = new DbGptDatasourceCreateRequest();
        request.setType(toDbGptType(datasource.getDatasourceType()));
        request.setDescription(firstNonBlank(datasource.getDescription(), datasource.getDatasourceName()));
        request.setParams(buildParams(datasource));
        if (datasource.getDbgptDatasourceId() != null) {
            try {
            dbGptClientService.deleteDatasource(datasource.getDbgptDatasourceId().intValue());
            } catch (Exception ignored) {
                // 远端可能已被手工删除，继续创建新的数据源。
            }
        }
        Integer remoteId = dbGptClientService.createDatasource(request);
        datasource.setDbgptDatasourceId(remoteId.longValue());
        datasource.setDbgptSyncStatus("SYNCED");
        datasource.setDbgptSyncMessage("同步成功");
        datasourceMgmtService.updateDatasource(datasource);
        return remoteId;
    }

    private Map<String, Object> buildParams(DatasourceDO datasource) {
        JSONObject config = StringUtils.isBlank(datasource.getDatasourceConfig())
                ? new JSONObject() : JSON.parseObject(datasource.getDatasourceConfig());
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("name", firstNonBlank(datasource.getDatasourceName(), "datamaster-" + datasource.getId()));
        params.put("host", datasource.getIp());
        params.put("port", datasource.getPort());
        params.put("user", firstNonBlank(config.getString("username"), config.getString("user")));
        params.put("password", decryptPassword(config.getString("password")));
        params.put("database", firstNonBlank(config.getString("dbname"), config.getString("database"), config.getString("schema")));
        params.put("schema", config.getString("schema"));
        return params;
    }

    private String decryptPassword(String password) {
        if (StringUtils.isBlank(password)) {
            return "";
        }
        try {
            return AesEncryptUtil.desEncrypt(password).trim();
        } catch (Exception e) {
            return password;
        }
    }

    private String toDbGptType(String type) {
        String value = StringUtils.defaultString(type).toLowerCase();
        if (value.contains("mysql")) {
            return "mysql";
        }
        if (value.contains("postgres")) {
            return "postgresql";
        }
        if (value.contains("kingbase")) {
            return "postgresql";
        }
        if (value.contains("dm")) {
            return "dm";
        }
        if (value.contains("oracle")) {
            return "oracle";
        }
        if (value.contains("sqlserver") || value.contains("sql server")) {
            return "mssql";
        }
        if (value.contains("hive")) {
            return "hive";
        }
        return value;
    }

    private void markFailed(DatasourceDO datasource, String message) {
        datasource.setDbgptSyncStatus("FAILED");
        datasource.setDbgptSyncMessage(normalizeAgentMessage(message));
        datasourceMgmtService.updateDatasource(datasource);
    }

    private String normalizeAgentMessage(String message) {
        if (StringUtils.isBlank(message)) {
            return message;
        }
        return message.replaceAll("(?i)DB[-_ ]?GPT", "决策智能体");
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (StringUtils.isNotBlank(value)) {
                return value;
            }
        }
        return "";
    }
}

