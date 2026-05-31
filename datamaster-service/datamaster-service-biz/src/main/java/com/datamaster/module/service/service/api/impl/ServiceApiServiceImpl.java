

package com.datamaster.module.service.service.api.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.Feature;
import com.alibaba.fastjson.serializer.*;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.parser.SimpleNode;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.StatementVisitorAdapter;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.util.SelectUtils;
import net.sf.jsqlparser.util.TablesNamesFinder;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.datamaster.common.core.domain.AjaxResult;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.database.DataSourceFactory;
import com.datamaster.common.database.DbQuery;
import com.datamaster.common.database.constants.DbQueryProperty;
import com.datamaster.common.database.constants.DbType;
import com.datamaster.common.database.core.DbColumn;
import com.datamaster.common.enums.ConfigType;
import com.datamaster.common.enums.DataConstant;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.JSONUtils;
import com.datamaster.common.utils.PageUtil;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.module.assets.api.datasource.dto.AssetsDatasourceRespDTO;
import com.datamaster.module.assets.api.sensitiveLevel.dto.AssetsSensitiveLevelRespDTO;
import com.datamaster.module.assets.api.service.asset.IAssetsDatasourceApiService;
import com.datamaster.module.assets.api.service.assetchild.api.IAssetsApiOutService;
import com.datamaster.module.assets.api.service.assetchild.gis.IAssetsAssetGisOutService;
import com.datamaster.module.service.api.service.api.ServiceApiService;
import com.datamaster.module.service.async.AsyncTask;
import com.datamaster.module.service.controller.admin.api.vo.*;
import com.datamaster.module.service.dal.dataobject.api.ServiceApiDO;
import com.datamaster.module.service.dal.dataobject.api.ExecuteConfig;
import com.datamaster.module.service.dal.dataobject.api.SqlParseDto;
import com.datamaster.module.service.dal.dataobject.apiLog.ServiceApiLogDO;
import com.datamaster.module.service.dal.dataobject.dto.ReqParam;
import com.datamaster.module.service.dal.dataobject.dto.ResParam;
import com.datamaster.module.service.dal.mapper.api.ServiceApiMapper;
import com.datamaster.module.service.service.api.IServiceApiService;
import com.datamaster.module.service.utils.JsonUtil;
import com.datamaster.module.service.utils.SqlBuilderUtil;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * API服务Service业务层处理
 *
 * @author lhs
 * @date 2025-02-12
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class ServiceApiServiceImpl extends ServiceImpl<ServiceApiMapper, ServiceApiDO> implements IServiceApiService, ServiceApiService {
    @Resource
    private ServiceApiMapper ServiceApiMapper;

    @Resource
    private IAssetsDatasourceApiService iAssetsDatasourceApiService;

    @Autowired
    private DataSourceFactory dataSourceFactory;

    @Resource
    private AsyncTask asyncTask;

    @Resource
    private IAssetsApiOutService iAssetsApiOutService;
    @Resource
    private IAssetsAssetGisOutService iAssetsAssetGisOutService;

    @Override
    public void releaseDataApi(String id, Long updateId, String updateBy) {
        // 获取详细信息
        ServiceApiDO dataApiEntity = ServiceApiMapper.selectById(id);
        ServiceApiLogDO apiLogEntity = null;
        try {
            invokeReleaseOrCancelApi(id, "1");

            LambdaUpdateWrapper<ServiceApiDO> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.set(ServiceApiDO::getStatus, DataConstant.EnableState.ENABLE.getKey());
            updateWrapper.set(ServiceApiDO::getUpdateBy, updateBy);
            updateWrapper.set(ServiceApiDO::getUpdateTime, LocalDateTime.now());
            updateWrapper.eq(ServiceApiDO::getId, id);
            ServiceApiMapper.update(null, updateWrapper);

            // 封装信息
            apiLogEntity = packApiLogEntity(
                    dataApiEntity, updateId, updateBy, id, 1, "", "1", 5);
        } catch (Exception e) {
            // 封装信息
            apiLogEntity = packApiLogEntity(
                    dataApiEntity, updateId, updateBy, id, 0, e.getMessage().toString(), "0", 5);
        } finally {
            // 封装信息进行异步存储日志
            // asyncTask.doTask(apiLogEntity);
        }

    }

    private void invokeReleaseOrCancelApi(String id, String type) {
        Map<String, Object> map = new HashMap<>(2);
        map.put("id", id);
        map.put("type", type);
        asyncTask.releaseOrCancelDataApi(map);
    }

    @Override
    public void cancelDataApi(String id, Long updateId, String updateBy) {
        // 获取详细信息
        ServiceApiDO dataApiEntity = ServiceApiMapper.selectById(id);
        ServiceApiLogDO apiLogEntity = null;
        try {
            invokeReleaseOrCancelApi(id, "2");

            LambdaUpdateWrapper<ServiceApiDO> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.set(ServiceApiDO::getStatus, DataConstant.EnableState.DISABLE.getKey());
            updateWrapper.set(ServiceApiDO::getUpdateBy, updateBy);
            updateWrapper.set(ServiceApiDO::getUpdateTime, LocalDateTime.now());
            updateWrapper.eq(ServiceApiDO::getId, id);
            ServiceApiMapper.update(null, updateWrapper);

            // 封装信息
            apiLogEntity = packApiLogEntity(
                    dataApiEntity, updateId, updateBy, id, 1, "", "1", 6);
        } catch (Exception e) {
            // 封装信息
            apiLogEntity = packApiLogEntity(
                    dataApiEntity, updateId, updateBy, id, 0, e.getMessage().toString(), "0", 6);
        } finally {
            // 封装信息进行异步存储日志
            // asyncTask.doTask(apiLogEntity);
        }

    }

    @Override
    public ServiceApiDO repeatFlag(JSONObject jsonObject) {
        LambdaQueryWrapperX<ServiceApiDO> queryWrapperX = new LambdaQueryWrapperX<>();
        queryWrapperX.eqIfPresent(ServiceApiDO::getName, jsonObject.getString("name"))
                .eqIfPresent(ServiceApiDO::getApiVersion, jsonObject.getString("apiVersion"))
                .eqIfPresent(ServiceApiDO::getApiUrl, jsonObject.getString("apiUrl"))
                .neIfPresent(ServiceApiDO::getId, jsonObject.getString("id"));
        ServiceApiDO ServiceApiDO = ServiceApiMapper.selectOne(queryWrapperX);
        return ServiceApiDO;
    }

    @Override
    public void queryServiceForwarding(HttpServletResponse response, ServiceApiReqVO ServiceApiReqVO) {
        Map<String, Object> result = JsonUtil.buildRequestObject(BeanUtils.toBean(ServiceApiReqVO, ServiceApiDO.class),
                ServiceApiReqVO.getQueryParams());
        String transmitType = ServiceApiReqVO.getTransmitType();
        if (org.apache.commons.lang3.StringUtils.equals("1", transmitType)) {
            iAssetsApiOutService.executeServiceForwarding(response, JSONUtils.convertToLong(ServiceApiReqVO.getApiId()), result);
        } else if (org.apache.commons.lang3.StringUtils.equals("2", transmitType)) {
            iAssetsAssetGisOutService.executeServiceForwarding(response, JSONUtils.convertToLong(ServiceApiReqVO.getApiId()),
                    result);
        } else {
            return;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult updateDataApi(ServiceApiDO dataApi) {
        String apiServiceType = dataApi.getApiServiceType();
        if (StringUtils.equals("3", apiServiceType)) {
            dataApiDaoUpdateById(dataApi);
            return AjaxResult.success();
        }
        ServiceApiDO apiDO = shareCode(dataApi);
        dataApiDaoUpdateById(apiDO);
        return AjaxResult.success();
    }

    /**
     * 保存API
     *
     * @param dataApi
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult saveDataApi(ServiceApiDO dataApi) {
        String apiServiceType = dataApi.getApiServiceType();
        if (StringUtils.equals("3", apiServiceType)) {
            dataApiDaoInsert(dataApi);
            return AjaxResult.success();
        }
        ServiceApiDO apiDO = shareCode(dataApi);
        dataApiDaoInsert(apiDO);
        return AjaxResult.success();
    }

    /**
     * 测试调用参数
     *
     * @param dataApi
     * @return
     */
    @Override
    public Object serviceTesting(ServiceApiDO dataApi) {
        ServiceApiDO dataApiEntity = shareCode(dataApi);
        List<ResParam> resParamsList = dataApiEntity.getResParamsList();
        Map<String, Object> params = dataApi.getParams();

        // 返回结果类型 1:分页 2:列表 3:详情-废弃
        // 返回结果类型;1：详情，2：列表，3：分页
        String resDataType = dataApiEntity.getResDataType();
        AssetsDatasourceRespDTO dataSource = iAssetsDatasourceApiService
                .getDatasourceById(Long.valueOf(dataApiEntity.getExecuteConfig().getSourceId()));

        DbQueryProperty dbQueryProperty = new DbQueryProperty(
                dataSource.getDatasourceType(),
                dataSource.getIp(),
                dataSource.getPort(),
                dataSource.getDatasourceConfig());
        DbQuery dbQuery = dataSourceFactory.createDbQuery(dbQueryProperty);
        // 参数

        Integer pageNum = Integer.parseInt(MapUtils.getString(params, "pageNum", "1"));
        Integer pageSize = Integer.parseInt(MapUtils.getString(params, "pageSize", "20"));
        PageUtil pageUtil = new PageUtil(pageNum, pageSize);
        Integer offset = pageUtil.getOffset();
        SqlBuilderUtil.SqlFilterResult sqlFilterResult;
        try {
            sqlFilterResult = SqlBuilderUtil.getInstance().applyFilters(dataApiEntity.getExecuteConfig().getSqlText(),
                    params);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            throw new ServiceException("API调用动态构造SQL语句出错");
        }
        Map<String, Object> acceptedFilters = sqlFilterResult.getAcceptedFilters();

        Object result = null;
        try {
            // Integer cacheSwitch = Integer.parseInt(dataApiEntity.getCacheSwitch());
            Integer cacheSwitch = 1;
            switch (resDataType) {
                case "3":
                    com.datamaster.common.database.core.PageResult<Map<String, Object>> pageResult = dbQuery
                            .queryByPage(sqlFilterResult.getSql(), acceptedFilters, offset, pageSize, cacheSwitch);
                    List<Map<String, Object>> data = pageResult.getData();
                    List<Map<String, Object>> list = this.encryptQueryResultList(data,
                            String.valueOf(dataApiEntity.getId()));
                    this.dateToStr(resParamsList, list);

                    pageResult.setPageNum(pageNum).setPageSize(pageSize).setData(list);
                    result = pageResult;
                    break;
                case "2":
                    List<Map<String, Object>> listResult = dbQuery.queryList(sqlFilterResult.getSql(), acceptedFilters,
                            cacheSwitch);
                    this.dateToStr(resParamsList, listResult);
                    result = this.encryptQueryResultList(listResult, String.valueOf(dataApiEntity.getId()));
                    break;
                case "1":
                    Map<String, Object> mapResult = dbQuery.queryOne(sqlFilterResult.getSql(), acceptedFilters,
                            cacheSwitch);
                    this.dateToStr(resParamsList, mapResult);
                    result = encryptQueryResultMap(mapResult, String.valueOf(dataApiEntity.getId()));
                    break;
            }
        } catch (Exception e) {
            throw new ServiceException("API调用查询结果集出错");
        } finally {
            dbQuery.close();
        }

        return JSON.parse(JSON.toJSONString(result, new ValueFilter() {
            @Override
            public Object process(Object o, String s, Object o1) {
                if (o1 instanceof Long) {
                    return String.valueOf(o1);
                }
                return o1;
            }
        }), Feature.OrderedField);
    }

    /**
     * 时间转换成字符串
     *
     * @param resParamsList
     * @param data
     */
    void dateToStr(List<ResParam> resParamsList, Object data) {
        try {
            if (data instanceof List) {
                List<Map<String, Object>> list = (List<Map<String, Object>>) data;
                list.forEach(map -> {
                    this.dateToStr(resParamsList, map);
                });
            } else {
                Map<String, Object> map = (Map<String, Object>) data;
                this.dateToStr(resParamsList, map);
            }
        } catch (Exception e) {
            log.error("时间转换成字符串出错", e);
        }
    }

    /**
     * 时间转换成字符串
     *
     * @param resParamsList
     * @param data
     */
    void dateToStr(List<ResParam> resParamsList, Map<String, Object> data) {
        try {
            resParamsList.forEach(resParam -> {
                if (com.datamaster.common.utils.StringUtils.isNotEmpty(resParam.getDataType()) && resParam.getDataType().equals("4") && data.get(resParam.getFieldName()) instanceof Date) {
                    data.put(resParam.getFieldName(), DateUtil.format((Date) data.get(resParam.getFieldName()), com.datamaster.common.utils.StringUtils.isNotEmpty(resParam.getDateFormat()) ? resParam.getDateFormat() : "yyyy-MM-dd HH:mm:ss"));
                }
            });
        } catch (Exception e) {
            log.error("时间转换成字符串出错", e);
        }
    }

    /**
     * 解析SQL
     *
     * @param sqlParseDto
     * @return
     */
    @Override
    public SqlParseVo sqlParse(SqlParseDto sqlParseDto) {

        String sourceId = sqlParseDto.getSourceId();
        String sql = sqlParseDto.getSqlText();
        sql = sql.replace(SqlBuilderUtil.getInstance().MARK_KEY_START, "");
        sql = sql.replace(SqlBuilderUtil.getInstance().MARK_KEY_END, "");
        Statement stmt;
        try {
            stmt = CCJSqlParserUtil.parse(sql);
        } catch (JSQLParserException e) {
            throw new ServiceException("SQL语法有问题，解析出错");
        }
        // 查询数据源信息
        AssetsDatasourceRespDTO datasourceById = iAssetsDatasourceApiService.getDatasourceById(Long.valueOf(sourceId));
        DbQueryProperty dbQueryProperty = new DbQueryProperty(
                datasourceById.getDatasourceType(),
                datasourceById.getIp(),
                datasourceById.getPort(),
                datasourceById.getDatasourceConfig());
        DbQuery dbQuery = dataSourceFactory.createDbQuery(dbQueryProperty);
        // 维护元数据缓存数据
        TablesNamesFinder tablesNamesFinder = new TablesNamesFinder();
        List<String> tables = tablesNamesFinder.getTableList(stmt);
        // 查询字段
        final List<Map<String, String>> cols = new ArrayList<>();
        // 查询参数
        final List<String> vars = new ArrayList<>();
        // 注掉单表解析 多表解析可以兼容单表
        multipleSqlParse(stmt, cols, vars, dbQuery);
        SqlParseVo sqlParseVo = new SqlParseVo();
        List<ReqParam> reqParams = vars.stream().map(s -> {
            ReqParam reqParam = new ReqParam();
            reqParam.setParamName(s);
            reqParam.setNullable(DataConstant.TrueOrFalse.FALSE.getKey());
            return reqParam;
        }).collect(Collectors.toList());

        sqlParseVo.setReqParams(reqParams);
        List<ResParam> resParams = new ArrayList<>();

        if (datasourceById != null) {
            Map<String, List<Map<String, String>>> map = cols.stream()
                    .collect(Collectors.groupingBy(e -> e.get("tableName").toString()));
            for (Map.Entry<String, List<Map<String, String>>> entry : map.entrySet()) {
                String entryKey = entry.getKey();
                List<Map<String, String>> entryValue = entry.getValue();
                String arr[] = entryKey.split("\\.");
                String tableName;
                String dbName;
                if (arr.length > 1) {
                    tableName = arr[1];
                    dbName = arr[0];
                } else {
                    tableName = arr[0];
                    dbName = null;
                }
                List<DbColumn> columns;
                if (org.apache.commons.lang3.StringUtils.isBlank(dbName)) {
                    columns = dbQuery.getTableColumns(dbQueryProperty, tableName);
                } else {
                    columns = dbQuery.getTableColumns(dbName, tableName);
                }
                Map<String, DbColumn> columnMap = columns.stream()
                        .collect(Collectors.toMap(k -> org.apache.commons.lang3.StringUtils
                                .replace(k.getColName(), "_", "").toUpperCase(Locale.ROOT), e -> e));
                entryValue.stream().forEach(m -> {
                    String columnName = m.get("columnName");
                    String columnAliasName = m.get("columnAliasName");
                    DbColumn dbColumn = columnMap.get(
                            org.apache.commons.lang3.StringUtils.replace(columnName, "_", "").toUpperCase(Locale.ROOT));
                    if (dbColumn != null) {
                        ResParam resParam = new ResParam();
                        resParam.setFieldName(
                                StrUtil.isNotBlank(columnAliasName) ? columnAliasName : dbColumn.getColName());
                        resParam.setFieldComment(
                                StrUtil.isNotBlank(dbColumn.getColComment()) ? dbColumn.getColComment() : "");
                        resParam.setDataType(StrUtil.isNotBlank(dbColumn.getDataType()) ? dbColumn.getDataType() : "");
                        resParam.setFieldAliasName(StrUtil.isNotBlank(columnAliasName) ? columnAliasName : "");
                        resParams.add(resParam);
                    }
                });
            }
        }
        sqlParseVo.setResParams(resParams);
        dbQuery.close();
        return sqlParseVo;
    }

    /**
     * 归纳修改入口
     *
     * @param dataApi
     */
    private void dataApiDaoUpdateById(ServiceApiDO dataApi) {
        ServiceApiLogDO apiLogEntity = null;
        try {
            baseMapper.updateById(dataApi);
            // 封装信息
            apiLogEntity = packApiLogEntity(
                    dataApi, dataApi.getUpdatorId(), dataApi.getUpdateBy(), dataApi.toString(), 1, "", "1", 3);
            if (StringUtils.equals("1",dataApi.getStatus())) {
                invokeReleaseOrCancelApi(String.valueOf(dataApi.getId()), "1");
            } else {
                invokeReleaseOrCancelApi(String.valueOf(dataApi.getId()), "2");
            }
        } catch (Exception e) {
            // 封装信息
            apiLogEntity = packApiLogEntity(
                    dataApi, dataApi.getUpdatorId(), dataApi.getUpdateBy(), dataApi.toString(), 0,
                    e.getMessage().toString(), "0", 3);
            throw new ServiceException("修改失败！");
        } finally {
            // 封装信息进行异步存储日志
            // asyncTask.doTask(apiLogEntity);
        }
    }

    /**
     * 归纳新增入口
     */
    private void dataApiDaoInsert(ServiceApiDO dataApi) {
        ServiceApiLogDO apiLogEntity = null;
        try {
            baseMapper.insert(dataApi);
            // 封装信息
            apiLogEntity = packApiLogEntity(
                    dataApi, dataApi.getCreatorId(), dataApi.getCreateBy(), dataApi.toString(), 1, "", "1", 2);
            if (StringUtils.equals("1",dataApi.getStatus())) {
                invokeReleaseOrCancelApi(String.valueOf(dataApi.getId()), "1");
            }
        } catch (Exception e) {
            dataApi.setId(0L);
            // 封装信息
            apiLogEntity = packApiLogEntity(
                    dataApi, dataApi.getCreatorId(), dataApi.getCreateBy(), dataApi.toString(), 0,
                    e.getMessage().toString(), "0", 2);
            throw new ServiceException("新增失败！");
        } finally {
            // 封装信息进行异步存储日志
            // asyncTask.doTask(apiLogEntity);
        }
    }

    /**
     * 封装 ApiLogEntity
     *
     * @return
     */
    private static ServiceApiLogDO packApiLogEntity(ServiceApiDO dataApiEntity, Long callerId, String callerBy,
                                               String callerParams,
                                               Integer callerSize, String msg, String status, Integer serviceType) {
        ServiceApiLogDO apiLogEntity = new ServiceApiLogDO();
        // id
        apiLogEntity.setApiId(dataApiEntity.getId());
        // 名字
        apiLogEntity.setApiName(dataApiEntity.getName());
        // 用户名字
        apiLogEntity.setCallerId(String.valueOf(callerId));
        // 用户id
        apiLogEntity.setCallerBy(callerBy);
        // 调取URL
        apiLogEntity.setCallerUrl(dataApiEntity.getApiUrl());
        // 调用参数
        apiLogEntity.setCallerParams(callerParams);
        // 调用数据量
        apiLogEntity.setCallerSize(callerSize);
        // 信息
        apiLogEntity.setMsg(msg);
        // 状态 0:失败，1：成功
        apiLogEntity.setStatus(Integer.valueOf(status));
        // 触发时间
        apiLogEntity.setCallerStartDate(LocalDateTime.now());
        apiLogEntity.setCallerIp("");
        // 服务类型 1: 请求, 2: 创建, 3: 修改, 4: 删除, 5: 发布, 6: 注销, 7: 浏览
        return apiLogEntity;
    }

    /**
     * 数据脱敏
     *
     * @param data
     * @param apiId
     * @return
     */
    private List<Map<String, Object>> encryptQueryResultList(List<Map<String, Object>> data, String apiId) {
        if (CollectionUtils.isEmpty(data)) {
            return data;
        }
        List<AssetsSensitiveLevelRespDTO> metadataDsnRuleLinkList = new ArrayList<>();
        try {
            // metadataDsnRuleLinkList =
            // metadataSourceServiceFeign.getMetadataDsnRuleLinkList(apiId);
        } catch (Exception e) {
            throw new ServiceException("API调用查询脱敏规则出错");
        }

        if (CollectionUtils.isEmpty(metadataDsnRuleLinkList)) {
            return data;
        }
        // 脱敏
        // for (Map<String, Object> datum : data) {
        // metadataDsnRuleLinkList.stream()
        // .filter(columnEntity -> datum.containsKey(columnEntity.getColumnName()))
        // .forEach(columnEntity -> {
        //
        // String columnName = columnEntity.getColumnName();
        // Object value = datum.get(columnName);
        //
        // if (value != null) {
        // MaskRuleUtil.MaskRule maskRule = MaskRuleUtil.mapToMaskRule(columnEntity);
        // Object object = MaskRuleUtil.processRule(value, maskRule);
        // datum.put(columnName, object);
        // }
        // });
        // }
        return data;
    }

    private Map<String, Object> encryptQueryResultMap(Map<String, Object> mapResult, String apiId) {
        if (MapUtils.isEmpty(mapResult)) {
            return mapResult;
        }

        List<AssetsSensitiveLevelRespDTO> metadataDsnRuleLinkList = new ArrayList<>();
        try {
            // metadataDsnRuleLinkList =
            // metadataSourceServiceFeign.getMetadataDsnRuleLinkList(apiId);
        } catch (Exception e) {
            throw new ServiceException("API调用查询脱敏规则出错");
        }

        if (CollectionUtils.isEmpty(metadataDsnRuleLinkList)) {
            return mapResult;
        }

        // metadataDsnRuleLinkList.stream()
        // .filter(columnEntity -> mapResult.containsKey(columnEntity.getColumnName()))
        // .forEach(columnEntity -> {
        // String columnName = columnEntity.getColumnName();
        // Object value = mapResult.get(columnName);
        //
        // if (value != null) {
        // MaskRuleUtil.MaskRule maskRule = MaskRuleUtil.mapToMaskRule(columnEntity);
        // Object object = MaskRuleUtil.processRule(value, maskRule);
        // mapResult.put(columnName, object);
        // }
        // });

        return mapResult;
    }

    private ServiceApiDO shareCode(ServiceApiDO dataApiDto) {
        if (dataApiDto.getExecuteConfig() == null
                || org.apache.commons.lang3.StringUtils.isBlank(dataApiDto.getExecuteConfig().getApiServiceType())) {
            return dataApiDto;
        }
        String configType = dataApiDto.getExecuteConfig().getApiServiceType();
        if (ConfigType.FORM.getKey().equals(configType)) {
            try {
                dataApiDto.getExecuteConfig().setSqlText(sqlJdbcNamedParameterBuild(dataApiDto));
            } catch (JSQLParserException e) {
                throw new ServiceException("SQL语法有问题，解析出错");
            }
        } else if (ConfigType.SCRIPT.getKey().equals(configType)) {
        }
        return dataApiDto;
    }

    private String sqlJdbcNamedParameterBuild(ServiceApiDO dataApi) throws JSQLParserException {
        String tableName = dataApi.getExecuteConfig().getTableName();
        ExecuteConfig executeConfig = dataApi.getExecuteConfig();
        if (StringUtils.isEmpty(executeConfig.getDbType())) {
            //通过数据源id获取
            AssetsDatasourceRespDTO dataSource = iAssetsDatasourceApiService.getDatasourceById(Long.valueOf(executeConfig.getSourceId()));
            if (dataSource == null) {
                throw new ServiceException("数据源不存在");
            }
            executeConfig.setDbType(dataSource.getDatasourceType());
            JSONObject dataSourceConfig = JSONObject.parseObject(dataSource.getDatasourceConfig());
            executeConfig.setDbName(dataSourceConfig.getString("dbname"));
            executeConfig.setSid(dataSourceConfig.getString("sid"));
        }
        if (org.apache.commons.lang3.StringUtils.equals(DbType.KINGBASE8.getDb(), executeConfig.getDbType())
                || org.apache.commons.lang3.StringUtils.equals(DbType.POSTGRE_SQL.getDb(), executeConfig.getDbType())
                || org.apache.commons.lang3.StringUtils.equals(DbType.SQL_SERVER.getDb(), executeConfig.getDbType())) {
            tableName = org.apache.commons.lang3.StringUtils.isNotBlank(executeConfig.getDbName()) ? executeConfig.getDbName() + "." + executeConfig.getSid() + "." + tableName : tableName;
        }  else {
            tableName = org.apache.commons.lang3.StringUtils.isNotBlank(executeConfig.getDbName()) ? executeConfig.getDbName() + "." + tableName : tableName;
        }
        Table table = new Table(tableName);
        String[] resParams = dataApi.getResParamsList().stream().map(s -> s.getFieldName()).toArray(String[]::new);
        Select select = SelectUtils.buildSelectFromTableAndExpressions(table, resParams);
        return SqlBuilderUtil.getInstance().buildHql(select.toString(), dataApi.getReqParamsList());
    }

    private void singleSqlParse(Statement stmt, List<Map<String, String>> cols, List<String> vars, String tableName,
                                DbQuery dbQuery) {
        stmt.accept(new StatementVisitorAdapter() {
            @Override
            public void visit(Select select) {
                select.getSelectBody().accept(new SelectVisitorAdapter() {
                    @Override
                    public void visit(PlainSelect plainSelect) {
                        plainSelect.getSelectItems().stream().forEach(selectItem -> {
                            selectItem.accept(new SelectItemVisitorAdapter() {
                                @Override
                                public void visit(SelectExpressionItem item) {
                                    Map<String, String> map = new HashMap<>();
                                    String columnName;
                                    SimpleNode node = item.getExpression().getASTNode();
                                    Object value = null;
                                    if (node != null) {
                                        value = node.jjtGetValue();
                                    }
                                    if (value == null) {
                                        Alias alias = item.getAlias();
                                        if (alias != null) {
                                            columnName = alias.getName();
                                        } else {
                                            columnName = String.valueOf(value);
                                            columnName = columnName.replace("'", "");
                                            columnName = columnName.replace("\"", "");
                                            columnName = columnName.replace("`", "");
                                        }
                                    } else if (value instanceof Column) {
                                        Column column = (Column) value;
                                        columnName = column.getColumnName();
                                        if (item.getAlias() != null) {
                                            map.put("columnAliasName", item.getAlias().getName());
                                        }
                                    } else if (value instanceof Function) {
                                        columnName = value.toString();
                                    } else {
                                        // 增加对select 'aaa' from table; 的支持
                                        columnName = String.valueOf(value);
                                        columnName = columnName.replace("'", "");
                                        columnName = columnName.replace("\"", "");
                                        columnName = columnName.replace("`", "");
                                    }
                                    columnName = columnName.replace("'", "");
                                    columnName = columnName.replace("\"", "");
                                    columnName = columnName.replace("`", "");
                                    map.put("tableName", tableName);
                                    map.put("columnName", columnName);
                                    cols.add(map);
                                }

                                @Override
                                public void visit(AllColumns allColumns) {
                                    List<DbColumn> columns = dbQuery.getTableColumns("", tableName);
                                    for (DbColumn column : columns) {
                                        Map<String, String> map = new HashMap<>();
                                        map.put("tableName", tableName);
                                        map.put("columnName", column.getColName());
                                        cols.add(map);
                                    }
                                }
                            });
                        });
                        Expression where = plainSelect.getWhere();
                        if (where != null) {
                            where.accept(new ExpressionVisitorAdapter() {
                                @Override
                                public void visit(JdbcNamedParameter jdbcNamedParameter) {
                                    vars.add(jdbcNamedParameter.getName());
                                }
                            });
                        }
                    }
                });
            }
        });
    }

    private void multipleSqlParse(Statement stmt, List<Map<String, String>> cols, List<String> vars, DbQuery dbQuery) {
        stmt.accept(new StatementVisitorAdapter() {
            @Override
            public void visit(Select select) {
                select.getSelectBody().accept(new SelectVisitorAdapter() {
                    @Override
                    public void visit(PlainSelect plainSelect) {
                        Map<String, String> aliasToTableMap = new HashMap<>();
                        FromItem fromItem = plainSelect.getFromItem();
                        if (fromItem instanceof Table) {
                            Table table = (Table) fromItem;
                            String tableName = table.getFullyQualifiedName();
                            if (table.getAlias() != null) {
                                aliasToTableMap.put(table.getAlias().getName(), tableName);
                            } else {
                                aliasToTableMap.put(table.getName(), tableName);
                            }
                        }

                        List<Join> joinList = plainSelect.getJoins();
                        if (joinList != null) {
                            for (Join join : joinList) {
                                FromItem joinItem = join.getRightItem();
                                if (joinItem instanceof Table) {
                                    Table table = (Table) joinItem;
                                    String tableName = table.getFullyQualifiedName();
                                    if (table.getAlias() != null) {
                                        aliasToTableMap.put(table.getAlias().getName(), tableName);
                                    } else {
                                        aliasToTableMap.put(table.getName(), tableName);
                                    }
                                }
                                if (join.getOnExpression() != null) {
                                    join.getOnExpression().accept(new ExpressionVisitorAdapter() {
                                        @Override
                                        public void visit(JdbcNamedParameter jdbcNamedParameter) {
                                            vars.add(jdbcNamedParameter.getName());
                                        }
                                    });
                                }
                            }
                        }

                        plainSelect.getSelectItems().forEach(selectItem -> {
                            selectItem.accept(new SelectItemVisitorAdapter() {
                                @Override
                                public void visit(SelectExpressionItem item) {
                                    Map<String, String> m = new HashMap<>();
                                    String columnName;
                                    String tableName = "";

                                    Expression expr = item.getExpression();
                                    if (expr instanceof Column) {
                                        Column column = (Column) expr;
                                        columnName = column.getColumnName();
                                        Table table = column.getTable();

                                        if (table != null && table.getName() != null) {
                                            // 有表前缀，解析 alias 到实际表名
                                            String tName = table.getName();
                                            tableName = aliasToTableMap.getOrDefault(tName, tName);
                                        } else if (aliasToTableMap.size() == 1) {
                                            // 无表前缀 + 单表查询，直接取唯一表
                                            tableName = aliasToTableMap.values().iterator().next();
                                        } else {
                                            // 无法判断归属表（多表 or 无 from），设为 unknown
                                            tableName = "unknown";
                                        }

                                        if (item.getAlias() != null) {
                                            m.put("columnAliasName", item.getAlias().getName());
                                        }
                                    } else if (expr instanceof Function) {
                                        columnName = expr.toString();
                                        tableName = columnName;
                                    } else if (expr instanceof SubSelect) {
                                        columnName = item.getAlias() != null ? item.getAlias().getName() : "subSelect";
                                        tableName = columnName;
                                        m.put("sub", "1");
                                    } else {
                                        columnName = expr.toString().replaceAll("[\"'`]", "");
                                    }

                                    columnName = columnName.replaceAll("[\"'`]", "");
                                    m.put("tableName", tableName);
                                    m.put("columnName", columnName);
                                    cols.add(m);
                                }

                                @Override
                                public void visit(AllTableColumns allTableColumns) {
                                    String alias = allTableColumns.getTable().getName();
                                    if (!aliasToTableMap.containsKey(alias))
                                        return;

                                    String fullTableName = aliasToTableMap.get(alias);
                                    String[] parts = fullTableName.split("\\.");
                                    String dbName = parts.length > 1 ? parts[0] : null;
                                    String tableName = parts.length > 1 ? parts[1] : parts[0];

                                    List<DbColumn> columns = dbQuery.getTableColumns(dbName, tableName);
                                    for (DbColumn column : columns) {
                                        Map<String, String> map = new HashMap<>();
                                        map.put("tableName", fullTableName);
                                        map.put("columnName", column.getColName());
                                        cols.add(map);
                                    }
                                }

                                @Override
                                public void visit(AllColumns allColumns) {
                                    for (Map.Entry<String, String> entry : aliasToTableMap.entrySet()) {
                                        String alias = entry.getKey();
                                        String fullTableName = entry.getValue();
                                        String[] parts = fullTableName.split("\\.");
                                        String dbName = parts.length > 1 ? parts[0] : null;
                                        String tableName = parts.length > 1 ? parts[1] : parts[0];

                                        List<DbColumn> columns = dbQuery.getTableColumns(dbName, tableName);
                                        for (DbColumn column : columns) {
                                            Map<String, String> map = new HashMap<>();
                                            map.put("tableName", fullTableName);
                                            map.put("columnName", column.getColName());
                                            cols.add(map);
                                        }
                                    }
                                }
                            });
                        });

                        Expression where = plainSelect.getWhere();
                        if (where != null) {
                            where.accept(new ExpressionVisitorAdapter() {
                                @Override
                                public void visit(JdbcNamedParameter jdbcNamedParameter) {
                                    vars.add(jdbcNamedParameter.getName());
                                }
                            });
                        }
                    }
                });
            }
        });
    }

    /**
     * 解析子查询
     *
     * @param select
     * @param cols
     * @param vars
     */
    void subSelectWhere(SubSelect select, List<Map<String, String>> cols, List<String> vars) {
        select.getSelectBody().accept(new SelectVisitorAdapter() {
            @Override
            public void visit(PlainSelect plainSelect) {
                // 存储表名
                Map<String, String> map = new HashMap<>();
                Table table = (Table) plainSelect.getFromItem();
                if (org.apache.commons.lang3.StringUtils.equals(table.getName().toUpperCase(Locale.ROOT), "DUAL")) {
                    return;
                }
                if (table.getAlias() != null) {
                    String tableName = table.getName();
                    if (org.apache.commons.lang3.StringUtils.isNotBlank(table.getSchemaName())) {
                        tableName = table.getSchemaName() + "." + tableName;
                    }
                    map.put(tableName, table.getAlias().getName());
                }
                if (plainSelect.getJoins() != null && plainSelect.getJoins().size() > 0) {
                    for (Join join : plainSelect.getJoins()) {
                        FromItem fromItem = join.getRightItem();
                        if (fromItem instanceof SubSelect) {
                            SubSelect subSelect = (SubSelect) fromItem;
                            subSelectWhere(subSelect, cols, vars);
                            continue;
                        }
                        join.getOnExpression().accept(new ExpressionVisitorAdapter() {
                            @Override
                            public void visit(JdbcNamedParameter jdbcNamedParameter) {
                                vars.add(jdbcNamedParameter.getName());
                            }
                        });
                        Table table1 = (Table) join.getRightItem();
                        if (table1.getAlias() != null) {
                            String tableName = table1.getName();
                            if (org.apache.commons.lang3.StringUtils.isNotBlank(table.getSchemaName())) {
                                tableName = table1.getSchemaName() + "." + tableName;
                            }
                            map.put(tableName, table1.getAlias().getName());
                        }
                    }
                }
                Expression where = plainSelect.getWhere();
                if (where != null) {
                    where.accept(new ExpressionVisitorAdapter() {
                        @Override
                        public void visit(JdbcNamedParameter jdbcNamedParameter) {
                            vars.add(jdbcNamedParameter.getName());
                        }
                    });
                }
            }
        });
    }

    @Override
    public PageResult<ServiceApiDO> getServiceApiPage(ServiceApiPageReqVO pageReqVO) {
        return ServiceApiMapper.selectPage(pageReqVO);
    }

    @Override
    public Long createServiceApi(ServiceApiSaveReqVO createReqVO) {
        ServiceApiDO dictType = BeanUtils.toBean(createReqVO, ServiceApiDO.class);
        ServiceApiMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public int updateServiceApi(ServiceApiSaveReqVO updateReqVO) {
        // 相关校验

        // 更新API服务
        ServiceApiDO updateObj = BeanUtils.toBean(updateReqVO, ServiceApiDO.class);
        return ServiceApiMapper.updateById(updateObj);
    }

    @Override
    public int removeServiceApi(Collection<Long> idList) {
        // 批量删除API服务
        return ServiceApiMapper.deleteBatchIds(idList);
    }

    @Override
    public ServiceApiDO getServiceApiById(Long id) {
        return ServiceApiMapper.selectById(id);
    }

    @Override
    public List<ServiceApiDO> getServiceApiList() {
        return ServiceApiMapper.selectList();
    }

    @Override
    public Map<Long, ServiceApiDO> getServiceApiMap() {
        List<ServiceApiDO> ServiceApiList = ServiceApiMapper.selectList();
        return ServiceApiList.stream()
                .collect(Collectors.toMap(
                        ServiceApiDO::getId,
                        ServiceApiDO -> ServiceApiDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing));
    }

    /**
     * 导入API服务数据
     *
     * @param importExcelList API服务数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName        操作用户
     * @return 结果
     */
    @Override
    public String importServiceApi(List<ServiceApiRespVO> importExcelList, boolean isUpdateSupport, String operName) {
        if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
            throw new ServiceException("导入数据不能为空！");
        }

        int successNum = 0;
        int failureNum = 0;
        List<String> successMessages = new ArrayList<>();
        List<String> failureMessages = new ArrayList<>();

        for (ServiceApiRespVO respVO : importExcelList) {
            try {
                ServiceApiDO ServiceApiDO = BeanUtils.toBean(respVO, ServiceApiDO.class);
                Long ServiceApiId = respVO.getId();
                if (isUpdateSupport) {
                    if (ServiceApiId != null) {
                        ServiceApiDO existingServiceApi = ServiceApiMapper.selectById(ServiceApiId);
                        if (existingServiceApi != null) {
                            ServiceApiMapper.updateById(ServiceApiDO);
                            successNum++;
                            successMessages.add("数据更新成功，ID为 " + ServiceApiId + " 的API服务记录。");
                        } else {
                            failureNum++;
                            failureMessages.add("数据更新失败，ID为 " + ServiceApiId + " 的API服务记录不存在。");
                        }
                    } else {
                        failureNum++;
                        failureMessages.add("数据更新失败，某条记录的ID不存在。");
                    }
                } else {
                    QueryWrapper<ServiceApiDO> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("id", ServiceApiId);
                    ServiceApiDO existingServiceApi = ServiceApiMapper.selectOne(queryWrapper);
                    if (existingServiceApi == null) {
                        ServiceApiMapper.insert(ServiceApiDO);
                        successNum++;
                        successMessages.add("数据插入成功，ID为 " + ServiceApiId + " 的API服务记录。");
                    } else {
                        failureNum++;
                        failureMessages.add("数据插入失败，ID为 " + ServiceApiId + " 的API服务记录已存在。");
                    }
                }
            } catch (Exception e) {
                failureNum++;
                String errorMsg = "数据导入失败，错误信息：" + e.getMessage();
                failureMessages.add(errorMsg);
                log.error(errorMsg, e);
            }
        }
        StringBuilder resultMsg = new StringBuilder();
        if (failureNum > 0) {
            resultMsg.append("很抱歉，导入失败！共 ").append(failureNum).append(" 条数据格式不正确，错误如下：");
            resultMsg.append("<br/>").append(String.join("<br/>", failureMessages));
            throw new ServiceException(resultMsg.toString());
        } else {
            resultMsg.append("恭喜您，数据已全部导入成功！共 ").append(successNum).append(" 条。");
        }
        return resultMsg.toString();
    }

    @Override
    public Long getCountByCatCode(String catCode) {
        return baseMapper.selectCount(Wrappers.lambdaQuery(ServiceApiDO.class)
                .likeRight(ServiceApiDO::getCatCode, catCode));
    }
}
