

package com.datamaster.module.service.service.api.impl;


import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.util.SelectUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.datamaster.common.database.DataSourceFactory;
import com.datamaster.common.database.DbQuery;
import com.datamaster.common.database.constants.DbQueryProperty;
import com.datamaster.common.database.constants.DbType;
import com.datamaster.common.database.core.PageResult;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.PageUtil;
import com.datamaster.common.datasource.mgmt.api.dto.DatasourceRespDTO;
import com.datamaster.module.assets.api.governance.dto.AssetsTableGovernanceReqDTO;
import com.datamaster.module.assets.api.governance.dto.AssetsTableGovernanceRespDTO;
import com.datamaster.common.datasource.mgmt.api.IDatasourceApiService;
import com.datamaster.module.assets.api.service.governance.IAssetsTableGovernanceApiService;
import com.datamaster.common.utils.SecurityUtils;
import com.datamaster.common.core.domain.model.LoginUser;
import com.datamaster.module.service.dal.dataobject.api.ServiceApiDO;
import com.datamaster.module.service.dal.dataobject.api.ExecuteConfig;
import com.datamaster.module.service.dal.dataobject.dto.ReqParam;
import com.datamaster.module.service.dal.dataobject.dto.ResParam;
import com.datamaster.module.service.utils.JsonUtil;
import com.datamaster.module.service.utils.SqlBuilderUtil;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ApiMappingEngine {

    /** 数据服务场景标识 */
    private static final String SCENE_DATA_SERVICE = "3";

    @Autowired
    private DataSourceFactory dataSourceFactory;

    @Resource
    private IDatasourceApiService iAssetsDatasourceApiService;

    @Resource
    private IAssetsTableGovernanceApiService assetsTableGovernanceApiService;


    public Object execute(ServiceApiDO dataApi, Map<String, Object> params) {
        DatasourceRespDTO dataSource;

        //返回结果类型 1:分页 2:列表 3:详情
        String resDataType = dataApi.getResDataType();
        //根据数据源id查询数据源信息
        String configJson = dataApi.getConfigJson();
        ExecuteConfig executeConfig = JSONObject.parseObject(configJson, ExecuteConfig.class);
        dataApi.setExecuteConfig(executeConfig);
        dataSource = iAssetsDatasourceApiService.getDatasourceById(Long.valueOf(executeConfig.getSourceId()));
        AssetsTableGovernanceRespDTO governanceResp = checkTableGovernance(dataApi, executeConfig);

        com.datamaster.common.database.constants.DbQueryProperty dbQueryProperty = new DbQueryProperty(
                dataSource.getDatasourceType(),
                dataSource.getIp(),
                dataSource.getPort(),
                dataSource.getDatasourceConfig()
        );
        DbQuery dbQuery = dataSourceFactory.createDbQuery(dbQueryProperty);

        // 参数
        //Integer pageNum = Integer.parseInt((String) params.getOrDefault("pageNum", 1));
        //Integer pageSize = Integer.parseInt((String) params.getOrDefault("pageSize", 20));
        Integer pageNum = Integer.parseInt(MapUtils.getString(params, "pageNum", "1"));
        Integer pageSize = Integer.parseInt(MapUtils.getString(params, "pageSize", "20"));
        PageUtil pageUtil = new PageUtil(pageNum, pageSize);
        Integer offset = pageUtil.getOffset();

        if(com.datamaster.common.utils.StringUtils.isEmpty(executeConfig.getSqlText())){
            try {
                String s = sqlJdbcNamedParameterBuild(dataApi, governanceResp);
                executeConfig.setSqlText(s);
            } catch (JSQLParserException e) {
                throw new RuntimeException(e);
            }
        }

        SqlBuilderUtil.SqlFilterResult sqlFilterResult;
        try {

            sqlFilterResult = SqlBuilderUtil.getInstance().applyFilters(dataApi.getExecuteConfig().getSqlText(), params);
        } catch (Exception e) {
            throw new ServiceException("API调用动态构造SQL语句出错");
        }
        Map<String, Object> acceptedFilters = sqlFilterResult.getAcceptedFilters();


        Object result = null;
        try {
//            Integer cacheSwitch = Integer.parseInt(dataApi.getCacheSwitch());
            Integer cacheSwitch = 0;
            switch (resDataType) {
                case "3":
                    PageResult<Map<String, Object>> pageResult = dbQuery.queryByPage(sqlFilterResult.getSql(), acceptedFilters, offset, pageSize, cacheSwitch);
                    List<Map<String, Object>> data = pageResult.getData();
                    List<Map<String, Object>> list = this.desensitizeData(data, governanceResp);

                    pageResult.setPageNum(pageNum).setPageSize(pageSize).setData(list);
                    result = pageResult;
                    break;
                case "2":
                    List<Map<String, Object>> listResult = dbQuery.queryList(sqlFilterResult.getSql(), acceptedFilters, cacheSwitch);
                    result = this.desensitizeData(listResult, governanceResp);
                    break;
                case "1":
                    Map<String, Object> mapResult = dbQuery.queryOne(sqlFilterResult.getSql(), acceptedFilters, cacheSwitch);
                    result = this.desensitizeSingleData(mapResult, governanceResp);
                    break;
            }
        } catch (Exception e) {
            throw new ServiceException("API调用查询结果集出错");
        }finally {
            dbQuery.close();
        }
        return result;
    }

    private AssetsTableGovernanceRespDTO checkTableGovernance(ServiceApiDO dataApi, ExecuteConfig executeConfig) {
        if (executeConfig == null || com.datamaster.common.utils.StringUtils.isEmpty(executeConfig.getSourceId())
                || com.datamaster.common.utils.StringUtils.isEmpty(executeConfig.getTableName())) {
            return null;
        }
        AssetsTableGovernanceReqDTO reqDTO = new AssetsTableGovernanceReqDTO();
        reqDTO.setDatasourceId(Long.valueOf(executeConfig.getSourceId()));
        reqDTO.setTableName(executeConfig.getTableName());
        reqDTO.setSpaceId(dataApi.getSpaceId());
        reqDTO.setSpaceCode(dataApi.getSpaceCode());
        reqDTO.setEntrance("DATA_SERVICE");
        AssetsTableGovernanceRespDTO respDTO = assetsTableGovernanceApiService.resolveTable(reqDTO);
        if (Boolean.FALSE.equals(respDTO.getAccessAllowed())) {
            throw new ServiceException(respDTO.getMessage());
        }
        return respDTO;
    }

    private String sqlJdbcNamedParameterBuild(ServiceApiDO dataApi, AssetsTableGovernanceRespDTO governanceResp) throws JSQLParserException {
        String tableName = dataApi.getExecuteConfig().getTableName();
        String resParams1 = dataApi.getResParams();
        String reqParams = dataApi.getReqParams();
        //转成
        List<ReqParam> reqParams1 = JSONArray.parseArray(reqParams, ReqParam.class);
        dataApi.setReqParamsList(reqParams1);
        List<ResParam> resParamsList = JSONArray.parseArray(resParams1, ResParam.class);
        resParamsList = filterAllowedColumns(resParamsList, governanceResp);
        resParamsList = filterDesensitizeHideColumns(resParamsList, governanceResp);
        dataApi.setResParamsList(resParamsList);
        ExecuteConfig executeConfig = dataApi.getExecuteConfig();
        if (com.datamaster.common.utils.StringUtils.isEmpty(executeConfig.getDbType())) {
            //通过数据源id获取
            DatasourceRespDTO dataSource = iAssetsDatasourceApiService.getDatasourceById(Long.valueOf(executeConfig.getSourceId()));
            if (dataSource == null) {
                throw new ServiceException("数据源不存在");
            }
            executeConfig.setDbType(dataSource.getDatasourceType());
            JSONObject dataSourceConfig = JSONObject.parseObject(dataSource.getDatasourceConfig());
            executeConfig.setDbName(dataSourceConfig.getString("dbname"));
            executeConfig.setSid(dataSourceConfig.getString("sid"));
        }
        if (org.apache.commons.lang3.StringUtils.equals(DbType.KINGBASE8.getDb(), executeConfig.getDbType())
                || org.apache.commons.lang3.StringUtils.equals(DbType.POSTGRE_SQL.getDb(), executeConfig.getDbType())) {
            tableName = org.apache.commons.lang3.StringUtils.isNotBlank(executeConfig.getSid()) ? executeConfig.getSid() + "." + tableName : tableName;
        } else if (org.apache.commons.lang3.StringUtils.equals(DbType.SQL_SERVER.getDb(), executeConfig.getDbType())) {
            if (org.apache.commons.lang3.StringUtils.isNotBlank(executeConfig.getDbName())
                    && org.apache.commons.lang3.StringUtils.isNotBlank(executeConfig.getSid())) {
                tableName = executeConfig.getDbName() + "." + executeConfig.getSid() + "." + tableName;
            } else if (org.apache.commons.lang3.StringUtils.isNotBlank(executeConfig.getDbName())) {
                tableName = executeConfig.getDbName() + "." + tableName;
            }
        }  else {
            tableName = org.apache.commons.lang3.StringUtils.isNotBlank(executeConfig.getDbName()) ? executeConfig.getDbName() + "." + tableName : tableName;
        }
        Table table = new Table(tableName);
        String[] resParams = dataApi.getResParamsList().stream().map(s -> s.getFieldName()).toArray(String[]::new);
        Select select = SelectUtils.buildSelectFromTableAndExpressions(table, resParams);
        return SqlBuilderUtil.getInstance().buildHql(select.toString(), dataApi.getReqParamsList());
    }

    /**
     * 按治理返回的正向可查字段过滤输出字段；无治理限制或未配置字段级权限时全量放行。
     */
    private List<ResParam> filterAllowedColumns(List<ResParam> resParamsList, AssetsTableGovernanceRespDTO governanceResp) {
        if (resParamsList == null || resParamsList.isEmpty() || governanceResp == null
                || governanceResp.getAllowedColumns() == null || governanceResp.getAllowedColumns().isEmpty()) {
            return resParamsList;
        }
        Set<String> allowed = governanceResp.getAllowedColumns().stream()
                .filter(Objects::nonNull)
                .map(column -> column.trim().toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
        List<ResParam> filtered = resParamsList.stream()
                .filter(resParam -> resParam.getFieldName() != null
                        && allowed.contains(resParam.getFieldName().trim().toLowerCase(Locale.ROOT)))
                .collect(Collectors.toList());
        if (filtered.isEmpty()) {
            throw new ServiceException("当前空间无权查询该服务的任何输出字段");
        }
        return filtered;
    }

    /**
     * 前置过滤脱敏隐藏列：敏感等级不足或脱敏规则隐藏的字段在 SQL 构建前剔除，
     * 避免敏感字段进入查询语句。无资产或未命中治理时原样返回。
     */
    private List<ResParam> filterDesensitizeHideColumns(List<ResParam> resParamsList, AssetsTableGovernanceRespDTO governanceResp) {
        if (resParamsList == null || resParamsList.isEmpty() || governanceResp == null || governanceResp.getAssetId() == null) {
            return resParamsList;
        }
        try {
            Long userId = SecurityUtils.getUserId();
            Long userPermissionLevel = currentUserPermissionLevel();
            Set<String> hideColumns = assetsTableGovernanceApiService.getDesensitizeHideColumns(
                    governanceResp.getAssetId(), userId, userPermissionLevel, SCENE_DATA_SERVICE);
            if (hideColumns == null || hideColumns.isEmpty()) {
                return resParamsList;
            }
            Set<String> hideLower = hideColumns.stream()
                    .filter(Objects::nonNull)
                    .map(column -> column.trim().toLowerCase(Locale.ROOT))
                    .collect(Collectors.toSet());
            List<ResParam> filtered = resParamsList.stream()
                    .filter(resParam -> resParam.getFieldName() == null
                            || !hideLower.contains(resParam.getFieldName().trim().toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
            if (filtered.isEmpty()) {
                throw new ServiceException("当前用户无权查询该服务的任何输出字段");
            }
            return filtered;
        } catch (Exception e) {
            log.warn("前置过滤脱敏隐藏列异常: {}", e.getMessage());
            return resParamsList;
        }
    }

    /**
     * 数据服务场景脱敏（列表）：委托治理公共方法，scene=3 数据服务。
     * governanceResp 为 null 或 assetId 为 null 时直接返回原数据，不影响查询链路。
     */
    private List<Map<String, Object>> desensitizeData(List<Map<String, Object>> data, AssetsTableGovernanceRespDTO governanceResp) {
        if (data == null || data.isEmpty() || governanceResp == null || governanceResp.getAssetId() == null) {
            return data;
        }
        try {
            Long userId = SecurityUtils.getUserId();
            Long userPermissionLevel = currentUserPermissionLevel();
            return assetsTableGovernanceApiService.desensitizeResultData(
                    governanceResp.getAssetId(), data, userId, userPermissionLevel, SCENE_DATA_SERVICE);
        } catch (Exception e) {
            log.warn("数据服务脱敏异常，返回原始数据: {}", e.getMessage());
            return data;
        }
    }

    /**
     * 数据服务场景脱敏（单条）
     */
    private Map<String, Object> desensitizeSingleData(Map<String, Object> mapData, AssetsTableGovernanceRespDTO governanceResp) {
        if (mapData == null || mapData.isEmpty() || governanceResp == null || governanceResp.getAssetId() == null) {
            return mapData;
        }
        try {
            List<Map<String, Object>> singleList = new ArrayList<>();
            singleList.add(mapData);
            List<Map<String, Object>> masked = desensitizeData(singleList, governanceResp);
            return (masked != null && !masked.isEmpty()) ? masked.get(0) : mapData;
        } catch (Exception e) {
            log.warn("数据服务单条脱敏异常，返回原始数据: {}", e.getMessage());
            return mapData;
        }
    }

    /**
     * 获取当前用户数据权限等级
     */
    private Long currentUserPermissionLevel() {
        try {
            LoginUser loginUser = SecurityUtils.getLoginUser();
            if (loginUser == null || loginUser.getUser() == null) {
                return null;
            }
            return loginUser.getUser().getDataPermissionLevel();
        } catch (Exception e) {
            log.warn("获取用户数据权限等级异常: {}", e.getMessage());
            return null;
        }
    }


    public static <K, V> Map.Entry<K, V> getIgnoreCaseData(Map<K, V> map, K key) {
        if (map == null || key == null) {
            return null;
        }

        for (Map.Entry<K, V> entry : map.entrySet()) {
            if (entry.getKey() != null && entry.getKey().toString().equalsIgnoreCase(key.toString())) {
                return entry;
            }
        }
        return null;
    }

    /**
     * 文件的访问（支持上传的文件和指定目录下的文件）
     *
     * @param api
     * @return
     */
    @SneakyThrows
    public void executeFileService(ServiceApiDO api, HttpServletResponse response) {
        //文件名称
//        String fileName = api.getFileName();
//        //文件路径或目录路径(目录时需将整个目录压缩返回)
//        String filePath = api.getFilePath();
//        // 设置响应内容类型
//        response.setContentType("application/octet-stream;charset=UTF-8");
//        File file = new File(filePath);
//        if (!file.exists()) {
//            throw new DataException("文件不存在！");
//        }
//        boolean delFlag = false;
//        if (file.isFile()) {
//            response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
//        } else if (file.isDirectory()) {
//            String zipFileName = UUID.fastUUID().toString() + ".zip";
//            response.setHeader("Content-Disposition", "attachment; filename=\"" + zipFileName + "\"");
//            String zipPath = file.getParentFile().getPath() + File.separator + zipFileName;
//            file = ZipUtil.zip(filePath, zipPath, true);
//            delFlag = true;
//        }
//        response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");
//        byte[] fileBytes = FileUtil.readBytes(file);
//        response.addHeader("Content-Length", "" + fileBytes.length);
//        IoUtil.write(response.getOutputStream(), true, fileBytes);
//        //删除文件
//        if (delFlag) {
//            FileUtil.del(file);
//        }
    }


    /**
     * 校验、打包、封装参数
     *
     * @param mapData
     * @param resType 回参type JSON、Map、List、不处理
     * @param api
     * @return
     */
    private static Object chackPackHttpData(String mapData, String resType, ServiceApiDO api) {
        //判断类型封装
        if (StringUtils.equals("不处理", resType)) {
            return mapData;
        }
        if (StringUtils.equals("JSON", resType)) {
            return mapData;
        }

        try {
            if (StringUtils.equals("Map", resType) && !StringUtils.isBlank(mapData)) {
                Map<String, Object> stringObjectMap = JsonUtil.parseJsonToMap(mapData);
                //判断是否有api的是否返回限制
                return JsonUtil.packFilterParameterOrMap(stringObjectMap, api);
            }

            if (StringUtils.equals("List", resType) && !StringUtils.isBlank(mapData)) {
                List<Object> maps = JsonUtil.parseJsonToListMap(mapData);
                return maps;
            }
        } catch (Exception e) {
            return mapData;
        }
        return mapData;

    }

    /**
     * 对于api进行校验，查看api是否禁用或者未查询到 等。。。
     *
     * @param yApiConfigEntity
     */
//    private static void chackYapiConfig(YApiConfigEntity yApiConfigEntity) {
//        //判断是否为null
//        if (yApiConfigEntity == null) {
//            throw new ServiceException("API调用，未查询到api配置");
//        }
//        //状态（0不启用，1启用）
//        if (!StringUtils.equals("1", yApiConfigEntity.getStatus())) {
//            throw new ServiceException("API调用，未查询到api未启用");
//        }
//    }

}
