package com.datamaster.module.service.service.api.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.datamaster.common.api.space.ISpaceApi;
import com.datamaster.common.datasource.mgmt.api.IDatasourceApiService;
import com.datamaster.common.datasource.mgmt.api.dto.DatasourceRespDTO;
import com.datamaster.common.enums.ConfigType;
import com.datamaster.common.enums.WhereType;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.SecurityUtils;
import com.datamaster.module.assets.api.governance.dto.AssetsTableGovernanceReqDTO;
import com.datamaster.module.assets.api.governance.dto.AssetsTableGovernanceRespDTO;
import com.datamaster.module.assets.api.service.governance.IAssetsTableGovernanceApiService;
import com.datamaster.module.governance.api.flow.ISpaceFlowApi;
import com.datamaster.module.service.dal.dataobject.api.ExecuteConfig;
import com.datamaster.module.service.dal.dataobject.api.ServiceApiDO;
import com.datamaster.module.service.dal.dataobject.dto.ReqParam;
import com.datamaster.module.service.dal.dataobject.dto.ResParam;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/** Shared API ownership and single-table source contract for publish, test and real consumption. */
@Component
public class ApiSourceGuard {
    @Resource private ISpaceApi spaces;
    @Resource private ISpaceFlowApi flows;
    @Resource private IDatasourceApiService datasources;
    @Resource private IAssetsTableGovernanceApiService governance;

    public void normalizeSpace(ServiceApiDO api, boolean requireMember) {
        if (api == null) throw new ServiceException("API不能为空");
        Long id = api.getSpaceId(); String code = api.getSpaceCode();
        if (id == null && code != null && !code.isBlank()) id = spaces.getSpaceIdBySpaceCode(code);
        if (id == null || id <= 0) throw new ServiceException("API缺少可信所属空间，请先维护归属，不能按未配置流程放行");
        String actual = spaces.getSpaceCodeBySpaceId(id);
        if (actual == null || actual.isBlank() || (code != null && !code.isBlank() && !actual.equals(code))) throw new ServiceException("API空间标识不一致或空间不存在");
        if (requireMember && !spaces.hasUserSpace(SecurityUtils.getUserId(), id, actual)) throw new ServiceException("无权维护或调试此空间API");
        api.setSpaceId(id); api.setSpaceCode(actual);
    }

    public void bindStoredSpace(ServiceApiDO request, ServiceApiDO stored) {
        if (stored == null || Boolean.TRUE.equals(stored.getDelFlag())) throw new ServiceException("API不存在或已删除");
        normalizeSpace(stored, true);
        if ((request.getSpaceId() != null && !stored.getSpaceId().equals(request.getSpaceId()))
                || (request.getSpaceCode() != null && !request.getSpaceCode().isBlank() && !stored.getSpaceCode().equals(request.getSpaceCode()))) {
            throw new ServiceException("不能通过编辑或调试更改API所属空间");
        }
        request.setSpaceId(stored.getSpaceId()); request.setSpaceCode(stored.getSpaceCode());
        if (request.getStatus() == null || request.getStatus().isBlank()) request.setStatus(stored.getStatus());
        if (request.getValidFlag() == null) request.setValidFlag(stored.getValidFlag());
    }

    public void hydrate(ServiceApiDO api) {
        if (api.getExecuteConfig() == null && api.getConfigJson() != null && !api.getConfigJson().isBlank()) api.setExecuteConfig(JSON.parseObject(api.getConfigJson(), ExecuteConfig.class));
        if (api.getResParamsList() == null) api.setResParamsList(api.getResParams() == null || api.getResParams().isBlank() ? new ArrayList<>() : JSON.parseArray(api.getResParams(), ResParam.class));
        if (api.getReqParamsList() == null) api.setReqParamsList(api.getReqParams() == null || api.getReqParams().isBlank() ? new ArrayList<>() : JSON.parseArray(api.getReqParams(), ReqParam.class));
        if (api.getReqParamsList() == null || api.getResParamsList() == null) throw new ServiceException("API字段配置不能为JSON null");
        if (api.getReqParamsList().size() > 100 || api.getResParamsList().size() > 200) throw new ServiceException("API参数字段超过100项或返回字段超过200项");
        for (ReqParam parameter : api.getReqParamsList()) if (parameter == null) throw new ServiceException("API参数字段不能为空");
        for (ResParam output : api.getResParamsList()) if (output == null) throw new ServiceException("API返回字段不能为空");
    }

    /** Files/forwarding cannot claim a single-table provenance. Reject them only for configured strict spaces. */
    public boolean assertNonQueryService(ServiceApiDO api) {
        boolean nonQuery = "4".equals(api.getApiServiceType()) || "1".equals(api.getTransmitType()) || "2".equals(api.getTransmitType());
        if (!nonQuery) return false;
        normalizeSpace(api, false);
        ISpaceFlowApi.Policy policy = flows.policy(api.getSpaceId(), api.getSpaceCode());
        if (policy.isAssetRequired() || policy.isQualityRequired()) throw new ServiceException("严格空间流程暂不支持无法核验全部来源的文件/转发服务");
        return true;
    }

    public void assertDatasourceSpace(ServiceApiDO context, Long datasourceId) {
        normalizeSpace(context, false);
        if (!datasources.hasDatasourceSpace(datasourceId, context.getSpaceId(), context.getSpaceCode())) throw new ServiceException("数据源未有效关联当前空间");
    }

    public void assertSqlInspectionAllowed(ServiceApiDO api) {
        normalizeSpace(api, true);
        ISpaceFlowApi.Policy policy = flows.policy(api.getSpaceId(), api.getSpaceCode());
        if (policy.isAssetRequired() || policy.isQualityRequired()) throw new ServiceException("严格空间暂不支持未核验全部来源的自定义SQL解析/预览，请使用表映射");
    }

    public AssetsTableGovernanceRespDTO check(ServiceApiDO api, ExecuteConfig config, String entrance) {
        normalizeSpace(api, false);
        assertNonQueryService(api);
        if ("4".equals(api.getApiServiceType())) return null;
        hydrate(api);
        if (config == null) config = api.getExecuteConfig();
        ISpaceFlowApi.Policy policy = flows.policy(api.getSpaceId(), api.getSpaceCode());
        boolean strict = policy.isAssetRequired() || policy.isQualityRequired();
        if (config == null) throw new ServiceException("API执行配置不能为空");
        boolean form = ConfigType.FORM.getKey().equals(config.getApiServiceType());
        if (!form && !ConfigType.SCRIPT.getKey().equals(config.getApiServiceType())) throw new ServiceException("API配置方式不合法，请明确表引导或脚本模式");
        if (strict && !form) throw new ServiceException("严格空间流程暂不支持无法核验全部来源的自定义SQL API，请使用表映射API");
        if (form) prepareForm(api, config);
        assertDatasourceSpace(api, sourceId(config));
        if (config.getSourceId() == null || config.getSourceId().isBlank() || config.getTableName() == null || config.getTableName().isBlank()) {
            if (strict || form) throw new ServiceException("API必须明确数据源和表");
            return null;
        }
        AssetsTableGovernanceReqDTO request = new AssetsTableGovernanceReqDTO();
        request.setDatasourceId(sourceId(config)); request.setTableName(config.getTableName());
        request.setSpaceId(api.getSpaceId()); request.setSpaceCode(api.getSpaceCode()); request.setEntrance(entrance);
        if (form) {
            List<String> columns = new ArrayList<>();
            for (ResParam field : api.getResParamsList()) columns.add(field.getFieldName());
            for (ReqParam field : api.getReqParamsList()) columns.add(field.getParamName());
            request.setColumnNames(columns.stream().distinct().collect(Collectors.toList()));
        }
        AssetsTableGovernanceRespDTO result = governance.resolveTable(request);
        if (result == null || !Boolean.TRUE.equals(result.getAccessAllowed())) throw new ServiceException(result == null ? "API来源准入检查未返回结论" : result.getMessage());
        if (form && result.getAllowedColumns() != null && !result.getAllowedColumns().isEmpty()) {
            Set<String> allowed = result.getAllowedColumns().stream().filter(Objects::nonNull).map(s -> s.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
            for (ReqParam filter : api.getReqParamsList()) {
                if (!allowed.contains(filter.getParamName().toLowerCase(Locale.ROOT))) throw new ServiceException("API过滤条件包含未授权字段");
            }
            List<ResParam> outputs = api.getResParamsList().stream().filter(s -> allowed.contains(s.getFieldName().toLowerCase(Locale.ROOT))).collect(Collectors.toList());
            if (outputs.isEmpty()) throw new ServiceException("当前空间无权查询该服务的任何输出字段");
            api.setResParamsList(outputs);
        }
        return result;
    }

    void prepareForm(ServiceApiDO api, ExecuteConfig config) {
        // Never execute a submitted/cached SQL string for FORM, even when it happens to be valid SQL.
        config.setSqlText(null);
        identifier(config.getTableName(), "表名");
        if (api.getResParamsList() == null || api.getResParamsList().isEmpty()) throw new ServiceException("表映射API必须明确返回字段，不能使用通配符");
        for (ResParam field : api.getResParamsList()) {
            if (field == null) throw new ServiceException("返回字段不能为空");
            identifier(field.getFieldName(), "返回字段");
        }
        for (ReqParam filter : api.getReqParamsList()) {
            if (filter == null) throw new ServiceException("过滤条件不能为空");
            identifier(filter.getParamName(), "过滤字段");
            if (Arrays.stream(WhereType.values()).noneMatch(type -> type.getType().equals(filter.getWhereType()))) {
                throw new ServiceException("过滤操作符不合法");
            }
        }
        DatasourceRespDTO source = datasources.getDatasourceById(sourceId(config));
        if (source == null || Boolean.FALSE.equals(source.getValidFlag())) throw new ServiceException("数据源不存在或已停用");
        JSONObject connection = JSON.parseObject(source.getDatasourceConfig());
        if (connection == null) throw new ServiceException("数据源配置不完整");
        // The registered connection is the authority for database/schema, not the API request.
        config.setDbType(source.getDatasourceType());
        config.setDbName(connection.getString("dbname"));
        String schema = connection.getString("sid");
        if (schema == null || schema.isBlank()) schema = connection.getString("schema");
        if ((schema == null || schema.isBlank()) && ("PostgreSQL".equalsIgnoreCase(source.getDatasourceType()) || "Kingbase8".equalsIgnoreCase(source.getDatasourceType()))) schema = "public";
        config.setSid(schema);
        if (config.getDbName() != null && !config.getDbName().isBlank()) identifier(config.getDbName(), "数据源库名");
        if (config.getSid() != null && !config.getSid().isBlank()) identifier(config.getSid(), "数据源schema");
    }

    private Long sourceId(ExecuteConfig config) {
        try { long id = Long.parseLong(config.getSourceId()); if (id > 0) return id; }
        catch (NumberFormatException ignored) { }
        throw new ServiceException("数据源ID不合法");
    }
    private void identifier(String value, String label) {
        if (value == null || !value.matches("[\\p{L}_][\\p{L}\\p{N}_$]*")) throw new ServiceException(label + "必须为单一标识符，暂不支持限定名、表达式或自定义SQL");
    }
}
