package com.datamaster.module.assets.service.datasource.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.datamaster.common.core.domain.AjaxResult;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.database.DataSourceFactory;
import com.datamaster.common.database.DbQuery;
import com.datamaster.common.database.constants.DbQueryProperty;
import com.datamaster.common.database.constants.DbType;
import com.datamaster.common.database.core.DbColumn;
import com.datamaster.common.database.core.DbName;
import com.datamaster.common.database.core.DbTable;
import com.datamaster.common.database.exception.DataQueryException;
import com.datamaster.common.enums.KingbaseColumnTypeEnum;
import com.datamaster.common.enums.MySqlColumnTypeEnum;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.AesEncryptUtil;
import com.datamaster.common.utils.DateUtils;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.module.taxonomy.api.project.ITaxonomyProjectApi;
import com.datamaster.module.taxonomy.api.project.dto.TaxonomyProjectReqDTO;
import com.datamaster.module.taxonomy.api.project.dto.TaxonomyProjectRespDTO;
import com.datamaster.module.assets.api.datasource.dto.AssetsDatasourceRespDTO;
import com.datamaster.module.assets.api.datasource.dto.DatasourceCreaTeTableListReqDTO;
import com.datamaster.module.assets.api.datasource.dto.DatasourceCreaTeTableReqDTO;
import com.datamaster.module.assets.api.service.asset.IAssetsDatasourceApiService;
import com.datamaster.module.assets.controller.admin.datasource.vo.AssetsDatasourcePageReqVO;
import com.datamaster.module.assets.controller.admin.datasource.vo.AssetsDatasourceRespVO;
import com.datamaster.module.assets.controller.admin.datasource.vo.AssetsDatasourceSaveReqVO;
import com.datamaster.module.assets.controller.admin.discovery.vo.AssetsDiscoveryColumnPageReqVO;
import com.datamaster.module.assets.controller.admin.discovery.vo.AssetsDiscoveryTablePageReqVO;
import com.datamaster.module.assets.controller.admin.discovery.vo.AssetsDiscoveryTaskLogSaveReqVO;
import com.datamaster.module.assets.controller.admin.discovery.vo.AssetsDiscoveryTaskRespVO;
import com.datamaster.module.assets.dal.dataobject.assetColumn.AssetsAssetColumnDO;
import com.datamaster.module.assets.dal.dataobject.datasource.AssetsDatasourceDO;
import com.datamaster.module.assets.dal.dataobject.datasource.AssetsDatasourceProjectRelDO;
import com.datamaster.module.assets.dal.dataobject.discovery.AssetsDiscoveryColumnDO;
import com.datamaster.module.assets.dal.dataobject.discovery.AssetsDiscoveryTableDO;
import com.datamaster.module.assets.dal.mapper.datasource.AssetsDatasourceMapper;
import com.datamaster.module.assets.service.datasource.IAssetsDatasourceProjectRelService;
import com.datamaster.module.assets.service.datasource.IAssetsDatasourceService;
import com.datamaster.module.assets.service.discovery.*;
import com.datamaster.module.standards.api.model.dto.StandardsModelColumnReqDTO;
import com.datamaster.module.standards.api.model.dto.StandardsModelColumnRespDTO;
import com.datamaster.module.standards.api.service.model.IStandardsModelApiService;
import com.datamaster.module.collector.api.service.etl.CollectorEtlTaskService;
import com.datamaster.module.system.service.ISysMessageService;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;
import com.datamaster.redis.service.IRedisService;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 数据源Service业务层处理 * * @author lhs * @date 2025-01-21
 */

@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class AssetsDatasourceServiceImpl extends ServiceImpl<AssetsDatasourceMapper, AssetsDatasourceDO> implements IAssetsDatasourceService, IAssetsDatasourceApiService {
    @Resource
    private AssetsDatasourceMapper AssetsDatasourceMapper;
    @Autowired
    private DataSourceFactory DataSourceFactory;
    @Resource
    private IStandardsModelApiService standardsModelApiService;
    @Resource
    private IAssetsDatasourceProjectRelService AssetsDatasourceProjectRelService;
    @Resource
    private ITaxonomyProjectApi attProjectApi;
    @Resource
    private CollectorEtlTaskService collectorEtlTaskService;
    @Autowired
    private IRedisService redisService;
    @Autowired
    private ISysMessageService iSysMessageService;
    @Autowired
    @Lazy
    private IAssetsDiscoveryTaskService IAssetsDiscoveryTaskService;
    @Autowired
    @Lazy
    private IAssetsDiscoveryColumnService IAssetsDiscoveryColumnService;
    @Autowired
    @Lazy
    private IAssetsDiscoveryTableService IAssetsDiscoveryTableService;
    @Autowired
    @Lazy
    private IAssetsDiscoveryTaskLogService IAssetsDiscoveryTaskLogService;
    @Resource
    private IAssetsDiscoveryLogBodyService IAssetsDiscoveryLogBodyService;

    /**
     * Redis      *     *      * 1.      * 2.  AssetsDatasourceDO.simplify()      * 3.  Redis Hash     *     *      * -  Worker  Redis      * -      *     *      * -  Redis  "datasource"
     */

    @PostConstruct
    public void initDatasourceCache() {
        try {
            List<AssetsDatasourceDO> list = AssetsDatasourceMapper.selectList();
            if (list == null || list.isEmpty()) {
                log.info("");
                return;
            }
// 可选：初始化前清空缓存
// redisService.del("datasource");
            for (AssetsDatasourceDO ds : list) {
                if (ds == null || ds.getId() == null) {
                    continue;
                }
                try {
// 最新数据源连接信息
                    String field = String.valueOf(ds.getId());
                    String value = com.alibaba.fastjson2.JSONObject.toJSONString(ds.simplify());
                    redisService.hashPut("datasource", field, value);

// todo 历史数据源连接信息 （临时处理，后续可以移除）
                    DbQueryProperty property = new DbQueryProperty(
                            ds.getDatasourceType(),
                            ds.getIp(),
                            ds.getPort(),
                            ds.getDatasourceConfig());
                    String key = property.trainToJdbcUrl();

// 判断是否已存在
                    Boolean exists = redisService.hashHasKey("datasource-old", key);
                    if (Boolean.FALSE.equals(exists)) {
                        redisService.hashPut("datasource-old", key, ds.getId().toString());
                        log.info("存入新历史数据源: key={}, value={}", key, ds.getId().toString());
                    } else {
                        log.info("已存在历史数据源: key={}，跳过存入", key);
                    }
                } catch (Exception e) {
                    log.warn("不支持转化的数据源");
                }
            }
            log.info("【数据源缓存初始化】成功加载 {} 条数据源到 Redis。", list.size());
        } catch (Exception e) {
            log.error("【数据源缓存初始化】加载 Redis 缓存失败：", e);
        }
    }

    /***     * 查询数据资产的数据源连接信息     *     * @param AssetsAsset     * @return     */

    @Override
    public List<AssetsDatasourceDO> getDataSourceByAsset(AssetsDatasourceRespVO AssetsAsset) {
        return AssetsDatasourceMapper.selectList();
    }

    @Override
    public PageResult<AssetsDatasourceDO> getDatasourcePage(AssetsDatasourcePageReqVO pageReqVO) {
        return AssetsDatasourceMapper.selectPage(pageReqVO);
    }

    @Override
    public PageResult<AssetsDatasourceDO> getDatasourceDppPage(AssetsDatasourcePageReqVO pageReqVO) {
        if (StringUtils.isEmpty(pageReqVO.getProjectCode())) {
            return new PageResult<AssetsDatasourceDO>();
        }
        AssetsDatasourceProjectRelDO assetsDatasourceProjectRelDO = new AssetsDatasourceProjectRelDO();
        assetsDatasourceProjectRelDO.setProjectCode(pageReqVO.getProjectCode());
        List<AssetsDatasourceProjectRelDO> AssetsDatasourceProjectRelList = AssetsDatasourceProjectRelService.getJoinProjectAndDatasource(assetsDatasourceProjectRelDO);
        if (AssetsDatasourceProjectRelList.isEmpty()) {
            return new PageResult<AssetsDatasourceDO>();
        }
        Map<Long, AssetsDatasourceProjectRelDO> datasourceProjectRelDOMap = AssetsDatasourceProjectRelList.stream().collect(Collectors.toMap(AssetsDatasourceProjectRelDO::getDatasourceId, AssetsDatasourceProjectRelDO1 -> AssetsDatasourceProjectRelDO1));
        List<Long> idList = datasourceProjectRelDOMap.keySet().stream().collect(Collectors.toList());
        pageReqVO.setIdList(idList);
        PageResult<AssetsDatasourceDO> AssetsDatasourceDOPageResult = AssetsDatasourceMapper.selectPage(pageReqVO);
        for (Object row : AssetsDatasourceDOPageResult.getRows()) {
            AssetsDatasourceDO AssetsDatasourceDO = (AssetsDatasourceDO) row;
            AssetsDatasourceProjectRelDO datasourceProjectRelDO = datasourceProjectRelDOMap.get(AssetsDatasourceDO.getId()) == null ? new AssetsDatasourceProjectRelDO() : datasourceProjectRelDOMap.get(AssetsDatasourceDO.getId());
            if (idList.contains(AssetsDatasourceDO.getId()) && !datasourceProjectRelDO.getDppAssigned()) {
                AssetsDatasourceDO.setIsAdminAddTo(false);
                AssetsDatasourceDO.setProjectName(datasourceProjectRelDO.getProjectName());
            }
        }
        return AssetsDatasourceDOPageResult;
    }

    @Override
    public List<AssetsDatasourceDO> getDatasourceList(AssetsDatasourcePageReqVO reqVO) {
        LambdaQueryWrapperX<AssetsDatasourceDO> AssetsDatasourceDOLambdaQueryWrapperX = new LambdaQueryWrapperX<>();
        AssetsDatasourceDOLambdaQueryWrapperX.likeIfPresent(AssetsDatasourceDO::getDatasourceName, reqVO.getDatasourceName()).like(StringUtils.isNotEmpty(reqVO.getDatasourceType()), AssetsDatasourceDO::getDatasourceType, reqVO.getDatasourceType()).eq(StringUtils.isNotEmpty(reqVO.getDatasourceConfig()), AssetsDatasourceDO::getDatasourceConfig, reqVO.getDatasourceConfig()).eq(StringUtils.isNotEmpty(reqVO.getIp()), AssetsDatasourceDO::getIp, reqVO.getIp());
        return AssetsDatasourceMapper.selectList(AssetsDatasourceDOLambdaQueryWrapperX);
    }

    @Override
    public Long createDatasource(AssetsDatasourceSaveReqVO createReqVO) {
        AssetsDatasourceDO dictType = BeanUtils.toBean(createReqVO, AssetsDatasourceDO.class);
        AssetsDatasourceMapper.insert(dictType);
        delAndSaveDatasourceProject(dictType);
        redisService.hashPut("datasource", dictType.getId().toString(), com.alibaba.fastjson2.JSONObject.toJSONString(this.getDatasourceDOById(dictType.getId()).simplify()));
        return dictType.getId();
    }

    @Override
    public int updateDatasource(AssetsDatasourceSaveReqVO updateReqVO) {
        Long datasourceId = updateReqVO.getId();

// 更新数据源
        AssetsDatasourceDO updateObj = BeanUtils.toBean(updateReqVO, AssetsDatasourceDO.class);
        delAndSaveDatasourceProject(updateObj);
        int i = AssetsDatasourceMapper.updateById(updateObj);
        redisService.hashPut("datasource", datasourceId.toString(), com.alibaba.fastjson2.JSONObject.toJSONString(this.getDatasourceDOById(datasourceId).simplify()));
        return i;
    }

    private void delAndSaveDatasourceProject(AssetsDatasourceDO AssetsDatasourceDO) {
        QueryWrapper<AssetsDatasourceProjectRelDO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("DATASOURCE_ID", AssetsDatasourceDO.getId());
        AssetsDatasourceProjectRelService.remove(queryWrapper);
        if (!AssetsDatasourceDO.getProjectList().isEmpty()) {
            for (AssetsDatasourceProjectRelDO AssetsDatasourceProjectRelDO : AssetsDatasourceDO.getProjectList()) {
                AssetsDatasourceProjectRelDO.setDatasourceId(AssetsDatasourceDO.getId());
                AssetsDatasourceProjectRelDO.setId(null);
            }
            AssetsDatasourceProjectRelService.saveBatch(AssetsDatasourceDO.getProjectList());
        }
    }

    @Override
    public int removeDatasource(Collection<Long> idList) {
// 批量删除数据源
        return AssetsDatasourceMapper.deleteBatchIds(idList);
    }

    @Override
    public int removeDatasourceDppOrDa(List<Long> idList, Long type) {
        int datasource = collectorEtlTaskService.checkTaskIdInDatasource(idList, null);
        if (datasource > 0) {
            throw new ServiceException(",!");
        }
        if (!idList.isEmpty()) {
            QueryWrapper<AssetsDatasourceProjectRelDO> queryWrapper = new QueryWrapper<>();
            queryWrapper.in("DATASOURCE_ID", idList);
            AssetsDatasourceProjectRelService.remove(queryWrapper);
        }
// 批量删除数据源
        return AssetsDatasourceMapper.deleteBatchIds(idList);
    }

    @Override
    public AssetsDatasourceRespDTO getDatasourceById(Long id) {
        AssetsDatasourceRespDTO dto = new AssetsDatasourceRespDTO();
        AssetsDatasourceDO AssetsDatasourceDO = AssetsDatasourceMapper.selectById(id);
        org.springframework.beans.BeanUtils.copyProperties(AssetsDatasourceDO, dto);
        return dto;
    }

    @Override
    public AssetsDatasourceDO getDatasourceDOById(Long id) {
        AssetsDatasourceDO AssetsDatasourceDO = AssetsDatasourceMapper.selectById(id);
        if (AssetsDatasourceDO == null) {
            return null;
        }
        AssetsDatasourceProjectRelDO AssetsDatasourceProjectRelDO = new AssetsDatasourceProjectRelDO();
        AssetsDatasourceProjectRelDO.setDatasourceId(AssetsDatasourceDO.getId());
        List<AssetsDatasourceProjectRelDO> AssetsDatasourceProjectRelList = AssetsDatasourceProjectRelService.getJoinProjectAndDatasource(AssetsDatasourceProjectRelDO);
        AssetsDatasourceDO.setProjectList(AssetsDatasourceProjectRelList);
        return AssetsDatasourceDO;
    }

    @Override
    public AssetsDatasourceRespVO getDatasourceByIdSimple(Long id) {
        return BeanUtils.toBean(AssetsDatasourceMapper.selectById(id), AssetsDatasourceRespVO.class);
    }

    @Override
    public List<AssetsDatasourceDO> getDatasourceList() {
        return AssetsDatasourceMapper.selectList();
    }

    @Override
    public Map<Long, AssetsDatasourceDO> getDatasourceMap() {
        List<AssetsDatasourceDO> AssetsDatasourceList = AssetsDatasourceMapper.selectList();
        return AssetsDatasourceList.stream().collect(Collectors.toMap(AssetsDatasourceDO::getId, AssetsDatasourceDO -> AssetsDatasourceDO,
// 保留已存在的值
                (existing, replacement) -> existing));
    }

    /***     * 导入数据源数据     *     * @param importExcelList 数据源数据列表     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据     * @param operName        操作用户     * @return 结果     */

    @Override
    public String importDatasource(List<AssetsDatasourceRespVO> importExcelList, boolean isUpdateSupport, String operName) {
        if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
            throw new ServiceException("");
        }
        int successNum = 0;
        int failureNum = 0;
        List<String> successMessages = new ArrayList<>();
        List<String> failureMessages = new ArrayList<>();
        for (AssetsDatasourceRespVO respVO : importExcelList) {
            try {
                AssetsDatasourceDO AssetsDatasourceDO = BeanUtils.toBean(respVO, AssetsDatasourceDO.class);
                Long AssetsDatasourceId = respVO.getId();
                if (isUpdateSupport) {
                    if (AssetsDatasourceId != null) {
                        AssetsDatasourceDO existingDatasource = AssetsDatasourceMapper.selectById(AssetsDatasourceId);
                        if (existingDatasource != null) {
                            AssetsDatasourceMapper.updateById(AssetsDatasourceDO);
                            successNum++;
                            successMessages.add("ID " + AssetsDatasourceId + " ");
                        } else {
                            failureNum++;
                            failureMessages.add("ID " + AssetsDatasourceId + " ");
                        }
                    } else {
                        failureNum++;
                        failureMessages.add("ID");
                    }
                } else {
                    QueryWrapper<AssetsDatasourceDO> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("id", AssetsDatasourceId);
                    AssetsDatasourceDO existingDatasource = AssetsDatasourceMapper.selectOne(queryWrapper);
                    if (existingDatasource == null) {
                        AssetsDatasourceMapper.insert(AssetsDatasourceDO);
                        successNum++;
                        successMessages.add("ID " + AssetsDatasourceId + " ");
                    } else {
                        failureNum++;
                        failureMessages.add("ID " + AssetsDatasourceId + " ");
                    }
                }
            } catch (Exception e) {
                failureNum++;
                String errorMsg = "" + e.getMessage();
                failureMessages.add(errorMsg);
                log.error(errorMsg, e);
            }
        }
        StringBuilder resultMsg = new StringBuilder();
        if (failureNum > 0) {
            resultMsg.append(" ").append(failureNum).append(" ");
            resultMsg.append("<br/>").append(String.join("<br/>", failureMessages));
            throw new ServiceException(resultMsg.toString());
        } else {
            resultMsg.append(" ").append(successNum).append(" ");
        }
        return resultMsg.toString();
    }

    @Override
    public AjaxResult clientsTest(Long id) {
        DbQuery dbQuery = this.buildDbQuery(id);
        if (dbQuery.valid()) {
            dbQuery.close();
            return AjaxResult.success("");
        }
        dbQuery.close();
        return AjaxResult.error("");
    }

    public DbQuery buildDbQuery(Long id) {
        AssetsDatasourceDO AssetsDatasourceBy = this.getDatasourceDOById(id);
        if (AssetsDatasourceBy == null) {
            throw new DataQueryException("");
        }
        DbQueryProperty dbQueryProperty = new DbQueryProperty(AssetsDatasourceBy.getDatasourceType(), AssetsDatasourceBy.getIp(), AssetsDatasourceBy.getPort(), AssetsDatasourceBy.getDatasourceConfig());
        return DataSourceFactory.createDbQuery(dbQueryProperty);
    }

    /**
     * @param id id     * @return
     */

    @Override
    public List<DbTable> getDbTables(Long id) {
        AssetsDatasourceDO AssetsDatasourceBy = this.getDatasourceDOById(id);
        if (AssetsDatasourceBy == null) {
            throw new DataQueryException("");
        }
        DbQueryProperty dbQueryProperty = new DbQueryProperty(AssetsDatasourceBy.getDatasourceType(), AssetsDatasourceBy.getIp(), AssetsDatasourceBy.getPort(), AssetsDatasourceBy.getDatasourceConfig());
        DbQuery dbQuery = DataSourceFactory.createDbQuery(dbQueryProperty);
        if (!dbQuery.valid()) {
            throw new DataQueryException("");
        }
        List<DbTable> tables = dbQuery.getTables(dbQueryProperty);
        dbQuery.close();
        return tables;
    }

    /**
     * *     * @param jsonObject id     * @return
     */

    @Override
    public List<StandardsModelColumnReqDTO> getColumnsList(JSONObject jsonObject) {
        List<StandardsModelColumnRespDTO> modelIdColumnList = new ArrayList<>();
        Boolean isOld = jsonObject.getStr("isOld") == null ? null : Boolean.valueOf(jsonObject.getStr("isOld"));
        if (isOld != null && !isOld && jsonObject.getStr("modelId") != null) {
            modelIdColumnList = standardsModelApiService.getModelIdColumnList(Long.valueOf(jsonObject.getStr("modelId")));
        }
        if (modelIdColumnList.size() > 0) {
            List<StandardsModelColumnReqDTO> columnReqDTOList = BeanUtils.toBean(modelIdColumnList, StandardsModelColumnReqDTO.class);
            return columnReqDTOList;
        }
// 获取数据库类型
        DbType dbTypeEnum = DbType.getDbType(jsonObject.getStr("type"));
        List<DbColumn> columnList = this.getDbTableColumns(Long.valueOf(jsonObject.getStr("id")), jsonObject.getStr("tableName"));
        List<StandardsModelColumnReqDTO> columnReqDTOList = new ArrayList<>();
        for (DbColumn column : columnList) {
            String dataType = column.getDataType();
            switch (dbTypeEnum) {
                case DM8:
                case ORACLE:
                    break;
                case MYSQL:
                    column.setDataType(MySqlColumnTypeEnum.convertToDmType(dataType));
                    break;
                case KINGBASE8:
                    column.setDataType(KingbaseColumnTypeEnum.convertToDmType(dataType));
                    break;
            }
            StandardsModelColumnReqDTO dpModelColumnReqDTO = new StandardsModelColumnReqDTO(column);
            columnReqDTOList.add(dpModelColumnReqDTO);
        }
        return columnReqDTOList;
    }

    @Override
    public List<AssetsAssetColumnDO> columnsAsAssetColumnList(JSONObject jsonObject) {
        List<DbColumn> columnsList = this.getDbTableColumns(Long.valueOf(jsonObject.getStr("id")), jsonObject.getStr("tableName"));
        return this.convertDbColumns(columnsList);
    }

    @Override
    public List<AssetsAssetColumnDO> columnsAsAssetColumnList(Long id, String tableName) {
        List<DbColumn> columnsList = this.getDbTableColumns(id, tableName);
        return convertDbColumns(columnsList);
    }

    /**
     * List<StandardsModelColumnReqDTO>  List<AssetsAssetColumnDO>     *     * @param columnsList StandardsModelColumnReqDTO      * @return  AssetsAssetColumnDO  null ArrayList
     */
    public static List<AssetsAssetColumnDO> convertDbColumns(List<DbColumn> columnsList) {
        if (columnsList == null || columnsList.isEmpty()) {
            return new ArrayList<>();
        }
        List<AssetsAssetColumnDO> assetColumns = new ArrayList<>(columnsList.size());
        for (DbColumn dbColumn : columnsList) {
// 利用 StandardsModelColumnReqDTO 的构造方法封装 DbColumn 到 DTO 对象
            StandardsModelColumnReqDTO dto = new StandardsModelColumnReqDTO(dbColumn);

// 利用 DTO 数据映射生成 AssetsAssetColumnDO 对象
            AssetsAssetColumnDO assetColumn = AssetsAssetColumnDO.builder()
// engName 映射为字段名称
                    .columnName(dto.getEngName())
// cnName 映射为字段注释
                    .columnComment(dto.getCnName()).columnType(dto.getColumnType()).columnLength(dto.getColumnLength()).columnScale(dto.getColumnScale()).nullableFlag(dto.getNullableFlag()).pkFlag(dto.getPkFlag()).defaultValue(dto.getDefaultValue()).build();
            assetColumns.add(assetColumn);
        }
        return assetColumns;
    }

    @Override
    public boolean creaDatasourceTeTable(DatasourceCreaTeTableReqDTO datasourceCreaTeTableReqDTO) {
        DbQueryProperty dbQueryProperty = new DbQueryProperty(datasourceCreaTeTableReqDTO.getDatasourceType(), datasourceCreaTeTableReqDTO.getIp(), datasourceCreaTeTableReqDTO.getPort(), datasourceCreaTeTableReqDTO.getDatasourceConfig());
        DbQuery dbQuery = DataSourceFactory.createDbQuery(dbQueryProperty);
        if (!dbQuery.valid()) {
            throw new DataQueryException("");
        }
        int tableStatus = dbQuery.generateCheckTableExistsSQL(dbQueryProperty, datasourceCreaTeTableReqDTO.getTableName());
        if (tableStatus > 0) {
            dbQuery.close();
            return false;
        }
        List<String> tableSQLList = dbQuery.generateCreateTableSQL(dbQueryProperty, datasourceCreaTeTableReqDTO.getTableName(), datasourceCreaTeTableReqDTO.getTableComment(), datasourceCreaTeTableReqDTO.getColumnsList());
        for (String sql : tableSQLList) {
            dbQuery.execute(sql);
        }
        dbQuery.close();
        return true;
    }

    @Override
    public boolean creaDatasourceTeTable(DbQuery dbQuery, DbQueryProperty dbQueryProperty, DatasourceCreaTeTableReqDTO datasourceCreaTeTableReqDTO) {
        int tableStatus = dbQuery.generateCheckTableExistsSQL(dbQueryProperty, datasourceCreaTeTableReqDTO.getTableName());
        if (tableStatus > 0) {
            return false;
        }
        List<String> tableSQLList = dbQuery.generateCreateTableSQL(dbQueryProperty, datasourceCreaTeTableReqDTO.getTableName(), datasourceCreaTeTableReqDTO.getTableComment(), datasourceCreaTeTableReqDTO.getColumnsList());
        for (String sql : tableSQLList) {
            dbQuery.execute(sql);
        }
        return true;
    }

    @Override
    public boolean creaDatasourceTeTableApi(DatasourceCreaTeTableReqDTO datasourceCreaTeTableReqDTO) {
        return this.creaDatasourceTeTable(datasourceCreaTeTableReqDTO);
    }

    @Override
    public boolean creaDatasourceTeTableApi(DbQuery dbQuery, DbQueryProperty dbQueryProperty, DatasourceCreaTeTableReqDTO creaTeTableReqDTO) {
        return this.creaDatasourceTeTable(dbQuery, dbQueryProperty, creaTeTableReqDTO);
    }

    @Override
    public boolean creaDatasourceTeTableListApi(DatasourceCreaTeTableListReqDTO datasourceCreaTeTableReqDTO) {
        DbQueryProperty dbQueryProperty = new DbQueryProperty(datasourceCreaTeTableReqDTO.getDatasourceType(), datasourceCreaTeTableReqDTO.getIp(), datasourceCreaTeTableReqDTO.getPort(), datasourceCreaTeTableReqDTO.getDatasourceConfig());
        DbQuery dbQuery = DataSourceFactory.createDbQuery(dbQueryProperty);
        List<DatasourceCreaTeTableReqDTO> dtoList = datasourceCreaTeTableReqDTO.getDtoList();
        if (CollectionUtils.isNotEmpty(dtoList)) {
            for (DatasourceCreaTeTableReqDTO creaTeTableReqDTO : dtoList) {
                this.creaDatasourceTeTable(dbQuery, dbQueryProperty, creaTeTableReqDTO);
            }
        }
        return true;
    }

    @Override
    public PageResult<TaxonomyProjectRespDTO> getNoDppAddList(TaxonomyProjectReqDTO pageReqVO) {
        PageResult<TaxonomyProjectRespDTO> attProjectPage = attProjectApi.getAttProjectPage(pageReqVO);
        Map<Long, AssetsDatasourceProjectRelDO> datasourceProjectRelDOMap = new HashMap<>();
        if (pageReqVO.getDatasourceId() != null) {
            AssetsDatasourceProjectRelDO assetsDatasourceProjectRelDO = new AssetsDatasourceProjectRelDO();
            assetsDatasourceProjectRelDO.setDatasourceId(pageReqVO.getDatasourceId());
            List<AssetsDatasourceProjectRelDO> AssetsDatasourceProjectRelList = AssetsDatasourceProjectRelService.getDatasourceProjectRelList(assetsDatasourceProjectRelDO);
            datasourceProjectRelDOMap = AssetsDatasourceProjectRelList.stream().collect(Collectors.toMap(AssetsDatasourceProjectRelDO::getProjectId, AssetsDatasourceProjectRelDO1 -> AssetsDatasourceProjectRelDO1));
        }
        for (Object row : attProjectPage.getRows()) {
            TaxonomyProjectRespDTO attProjectRespDTO = (TaxonomyProjectRespDTO) row;
            Boolean dppAssigned = datasourceProjectRelDOMap.get(attProjectRespDTO.getId()) != null && datasourceProjectRelDOMap.get(attProjectRespDTO.getId()).getDppAssigned();
            attProjectRespDTO.setDppAssigned(dppAssigned);
        }
        return attProjectPage;
    }

    @Override
    public List<AssetsDatasourceDO> getDatasourceDppNoKafka(AssetsDatasourcePageReqVO AssetsDatasource) {
        List<Long> idList = new ArrayList<>();
        Map<Long, AssetsDatasourceProjectRelDO> datasourceProjectRelDOMap = new HashMap<>();
        if (StringUtils.isNotEmpty(AssetsDatasource.getProjectCode())) {
            AssetsDatasourceProjectRelDO assetsDatasourceProjectRelDO = new AssetsDatasourceProjectRelDO();
            assetsDatasourceProjectRelDO.setProjectCode(AssetsDatasource.getProjectCode());
            List<AssetsDatasourceProjectRelDO> AssetsDatasourceProjectRelList = AssetsDatasourceProjectRelService.getJoinProjectAndDatasource(assetsDatasourceProjectRelDO);
            if (AssetsDatasourceProjectRelList.isEmpty()) {
                return new ArrayList<>();
            }
            datasourceProjectRelDOMap = AssetsDatasourceProjectRelList.stream().collect(Collectors.toMap(AssetsDatasourceProjectRelDO::getDatasourceId, AssetsDatasourceProjectRelDO1 -> AssetsDatasourceProjectRelDO1));
            idList = datasourceProjectRelDOMap.keySet().stream().collect(Collectors.toList());
            AssetsDatasource.setIdList(idList);
        }
        LambdaQueryWrapperX<AssetsDatasourceDO> queryWrapperX = new LambdaQueryWrapperX<>();
        queryWrapperX.inIfPresent(AssetsDatasourceDO::getId, idList).neIfPresent(AssetsDatasourceDO::getDatasourceType, "Kafka").likeIfPresent(AssetsDatasourceDO::getDatasourceType, AssetsDatasource.getDatasourceType()).likeIfPresent(AssetsDatasourceDO::getDatasourceName, AssetsDatasource.getDatasourceName());
        List<AssetsDatasourceDO> datasourceDOList = AssetsDatasourceMapper.selectList(queryWrapperX);
        for (AssetsDatasourceDO AssetsDatasourceDO : datasourceDOList) {
            AssetsDatasourceProjectRelDO datasourceProjectRelDO = datasourceProjectRelDOMap.get(AssetsDatasourceDO.getId()) == null ? new AssetsDatasourceProjectRelDO() : datasourceProjectRelDOMap.get(AssetsDatasourceDO.getId());
            if (idList.contains(AssetsDatasourceDO.getId()) && !datasourceProjectRelDO.getDppAssigned()) {
                AssetsDatasourceDO.setIsAdminAddTo(false);
                AssetsDatasourceDO.setProjectName(datasourceProjectRelDO.getProjectName());
            }
        }
        return datasourceDOList;
    }

    @Override
    public com.datamaster.common.database.core.PageResult<Map<String, Object>> executeSqlQuery(AssetsDatasourcePageReqVO AssetsDatasource) {
        String sqlText = decryptSqlText(AssetsDatasource.getSqlText());
        DbQuery dbQuery = getDbQuery(AssetsDatasource);
        int[] paging = getPagingParameters(AssetsDatasource);

// paging 数组中：paging[0] 为 offset，paging[1] 为 pageSize
        com.datamaster.common.database.core.PageResult<Map<String, Object>> mapPageResult = dbQuery.queryByPage(sqlText, paging[0], paging[1]);
        dbQuery.close();
        return mapPageResult;
    }

    @SneakyThrows
    @Override
    public void exportSqlQueryResult(HttpServletResponse response, AssetsDatasourcePageReqVO AssetsDatasource) {
        String sqlText = decryptSqlText(AssetsDatasource.getSqlText());
        DbQuery dbQuery = getDbQuery(AssetsDatasource);
        int[] paging = getPagingParameters(AssetsDatasource);
        com.datamaster.common.database.core.PageResult<Map<String, Object>> result = dbQuery.queryByPage(sqlText, paging[0], paging[1]);
        dbQuery.close();
        List<Map<String, Object>> dataList = result.getData();

// 移除每条记录中的 ROW_ID 字段
        dataList.forEach(map -> map.remove("ROW_ID"));
        String schemeName = "导出第" + paging[2] + "页数据-" + IdUtil.simpleUUID();
        exportByList(response, dataList, schemeName);
    }

    @Override
    public List<DbColumn> sqlParse(String sourceId, String sqlText) {
        Statement stmt;
        try {
            stmt = CCJSqlParserUtil.parse(sqlText);
        } catch (JSQLParserException e) {
            throw new ServiceException("SQL");
        }
// 查询数据源信息
        AssetsDatasourceRespDTO datasourceById = this.getDatasourceById(Long.valueOf(sourceId));
        DbQueryProperty dbQueryProperty = new DbQueryProperty(
                datasourceById.getDatasourceType(),
                datasourceById.getIp(),
                datasourceById.getPort(),
                datasourceById.getDatasourceConfig());
        DbQuery dbQuery = DataSourceFactory.createDbQuery(dbQueryProperty);
        return dbQuery.getColumnsByQuerySql(sqlText);
    }

    /***     * 解密传入的 SQL 语句     */
    private String decryptSqlText(String encryptedSqlText) {
        String sqlText = "";
        try {
//
            sqlText = encryptedSqlText;
            sqlText = AesEncryptUtil.desEncrypt(encryptedSqlText).trim();
        } catch (Exception e) {
            throw new RuntimeException("执行语句解密异常，请联系管理员！", e);
        }
        if (sqlText == null || sqlText.isEmpty()) {
            throw new DataQueryException("SQL语句不能为空");
        }
// 检查是否包含分隔符';'
        int semicolonCount = sqlText.length() - sqlText.replace(";", "").length();
        if (semicolonCount > 0) {
            int firstIndex = sqlText.indexOf(";");
            int lastIndex = sqlText.lastIndexOf(";");

// 若';'不只出现在末尾，则视为存在多个SQL语句
            if (firstIndex != lastIndex || lastIndex != sqlText.length() - 1) {
                throw new DataQueryException("仅支持单个查询SQL语句，不允许存在多个语句或中间使用';'分隔");
            }
// 移除末尾的';'
            sqlText = sqlText.substring(0, sqlText.length() - 1).trim();
            if (sqlText.contains(";")) {
                throw new DataQueryException("仅支持单个查询SQL语句，不允许存在多个语句或中间使用';'分隔");
            }
        }
// 确保SQL以"select"开头（忽略大小写）
        if (!sqlText.toLowerCase().startsWith("select")) {
            throw new DataQueryException("仅允许执行查询操作的SQL语句");
        }
// 进一步检测是否包含非查询的SQL标识
        validateQueryOnly(sqlText);
        return sqlText;
    }

    /***     * SQL     * SQL     */
    private void validateQueryOnly(String sqlText) {
// 移除SQL中的字符串常量，避免因字符串中包含敏感词导致误判
        String withoutStringLiterals = sqlText.replaceAll("'[^']*'", "");

// 定义不允许出现的关键词（全词匹配，忽略大小写）
        String[] forbiddenKeywords = {"insert", "update", "delete", "create", "drop", "alter", "truncate", "exec", "execute", "merge"};
        String lowerSql = withoutStringLiterals.toLowerCase();
        for (String keyword : forbiddenKeywords) {
// \b 确保匹配关键字边界，避免匹配到部分字段
            if (Pattern.compile("\\b" + keyword + "\\b").matcher(lowerSql).find()) {
                throw new DataQueryException("SQL语句中包含非查询操作标识: " + keyword);
            }
        }
    }

    /***     * 根据请求中的数据源 ID 获取 DbQuery 对象     */
    private DbQuery getDbQuery(AssetsDatasourcePageReqVO AssetsDatasource) {
        AssetsDatasourceDO datasource = this.getDatasourceDOById(AssetsDatasource.getId());
        if (datasource == null) {
            throw new DataQueryException("");
        }
        DbQueryProperty property = new DbQueryProperty(datasource.getDatasourceType(), datasource.getIp(), datasource.getPort(), datasource.getDatasourceConfig());
        DbQuery dbQuery = DataSourceFactory.createDbQuery(property);
        if (!dbQuery.valid()) {
            throw new DataQueryException("");
        }
        return dbQuery;
    }

    /**
     * int      * paging[0]  offset     * paging[1]  pageSize     * paging[2]  pageNum
     */
    private int[] getPagingParameters(AssetsDatasourcePageReqVO AssetsDatasource) {
        int pageSize = AssetsDatasource.getPageSize() != null ? AssetsDatasource.getPageSize() : 20;
        int pageNum = AssetsDatasource.getPageNum() != null ? AssetsDatasource.getPageNum() : 1;
        int offset = pageNum > 0 ? (pageNum - 1) * pageSize : 0;
        return new int[]{offset, pageSize, pageNum};
    }

    @SneakyThrows
    private static void exportByList(HttpServletResponse response, List<Map<String, Object>> dataList, String tableName) {
        if (dataList == null) {
            throw new RuntimeException("");
        }
// 获取第一行数据的所有列名作为 order
        Map<String, Object> firstRow = dataList.get(0);

// 使用 Set 可以确保列名唯一性
        List<String> order = new ArrayList<>(firstRow.keySet());

//1.创建工作博
        XSSFWorkbook workbook = new XSSFWorkbook();

//头部字段字体
        XSSFFont headFont = workbook.createFont();

//字体高度
        headFont.setFontHeightInPoints((short) 24);

//字体
        headFont.setFontName("宋体");
        headFont.setBold(true);

// 设置单元格类型
        XSSFCellStyle headCellStyle = workbook.createCellStyle();
        headCellStyle.setFont(headFont);

//水平布局：居中
        headCellStyle.setAlignment(HorizontalAlignment.CENTER);

//垂直居中
        headCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        headCellStyle.setWrapText(true);

//标注划字体
        XSSFFont font = workbook.createFont();

//字体高度
        font.setFontHeightInPoints((short) 11);

//字体
        font.setFontName("宋体");

//列样式
        XSSFCellStyle cellStyle = workbook.createCellStyle();
        cellStyle.setFont(font);

//水平布局：居中
        cellStyle.setAlignment(HorizontalAlignment.CENTER);

//垂直居中
        cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        cellStyle.setWrapText(true);

//2。创建工作表
        XSSFSheet sheet = workbook.createSheet(tableName);

//冻结第一行
        sheet.createFreezePane(0, 1, 0, 1);

//3。创建字段行
        XSSFRow row = sheet.createRow(0);
        row.setHeight((short) (2 * 200));
        int index = 0;
        for (String key : order) {
//设置默认宽度
            sheet.setColumnWidth(index, 25 * 256);
            XSSFCell labelCell = row.createCell(index);
            labelCell.setCellStyle(cellStyle);
            labelCell.setCellValue(key);
            index++;
        }
//4。数据行
        for (int i = 0; i < dataList.size(); i++) {
            Map<String, Object> map = dataList.get(i);

//数据行
            XSSFRow dataRow = sheet.createRow(i + 1);
            dataRow.setHeight((short) (4 * 200));
            int columnIndex = 0;
            for (String key : order) {
                Object valueObj = map.get(key);
                String value = "";
                if (valueObj instanceof Date) {
// 如果是日期类型，转换为固定格式的字符串
                    SimpleDateFormat DateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    value = DateFormat.format((Date) valueObj);
                } else {
                    value = String.valueOf(valueObj);
                }
                XSSFCell labelCell = dataRow.createCell(columnIndex);
                labelCell.setCellStyle(cellStyle);
                labelCell.setCellValue(value);
                columnIndex++;
            }
        }
        if (response != null) {
// 5.输出流 输出
            ByteArrayOutputStream baos = null;
            try {
                baos = new ByteArrayOutputStream();
                workbook.write(baos);
                baos.flush();
                byte[] aa = baos.toByteArray();
                response.setCharacterEncoding("UTF-8");
                response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(tableName + ".xls", "UTF-8"));
                response.setHeader("Content-Type", "application/vnd.ms-excel");
                response.setCharacterEncoding("UTF-8");
                response.getOutputStream().write(aa);
                response.setContentLength(aa.length);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("系统错误");
            } finally {
                if (response.getOutputStream() != null) {
                    response.getOutputStream().flush();
                }
//6.刷新流，释放资源
                if (baos != null) {
                    baos.flush();
                    baos.close();
                }
//关闭资源
                workbook.close();
            }
        }
    }

    @Override
    public Boolean editDatasourceStatus(Long datasourceId, Long status) {
        return this.update(Wrappers.lambdaUpdate(AssetsDatasourceDO.class).eq(AssetsDatasourceDO::getId, datasourceId).set(AssetsDatasourceDO::getValidFlag, status));
    }

    /**
     * @param id id     * @param tableName      * @return
     */

    @Override
    public List<DbColumn> getDbTableColumns(Long id, String tableName) {
        if (StringUtils.isEmpty(tableName)) {
            throw new DataQueryException("");
        }
        AssetsDatasourceDO AssetsDatasourceBy = this.getDatasourceDOById(id);
        if (AssetsDatasourceBy == null) {
            throw new DataQueryException("");
        }
        DbQueryProperty dbQueryProperty = new DbQueryProperty(AssetsDatasourceBy.getDatasourceType(), AssetsDatasourceBy.getIp(), AssetsDatasourceBy.getPort(), AssetsDatasourceBy.getDatasourceConfig());
        DbQuery dbQuery = DataSourceFactory.createDbQuery(dbQueryProperty);
        if (!dbQuery.valid()) {
            throw new DataQueryException("");
        }
        List<DbColumn> tableColumns = dbQuery.getTableColumns(dbQueryProperty, tableName);
        dbQuery.close();
        return tableColumns;
    }

    @Override
    public DbTable getDbTable(Long datasourceId, String tableName) {
        AssetsDatasourceDO AssetsDatasourceBy = this.getDatasourceDOById(datasourceId);
        if (AssetsDatasourceBy == null) {
            throw new DataQueryException("");
        }
        DbQueryProperty dbQueryProperty = new DbQueryProperty(AssetsDatasourceBy.getDatasourceType(), AssetsDatasourceBy.getIp(), AssetsDatasourceBy.getPort(), AssetsDatasourceBy.getDatasourceConfig());
        DbQuery dbQuery = DataSourceFactory.createDbQuery(dbQueryProperty);
        if (!dbQuery.valid()) {
            throw new DataQueryException("");
        }
        List<DbTable> tables = dbQuery.getTables(dbQueryProperty);
        if (StringUtils.isNotEmpty(tableName)) {
            tables = tables.stream().filter(dbTable -> org.apache.commons.lang3.StringUtils.indexOfIgnoreCase(dbTable.getTableName(), tableName) > -1).collect(Collectors.toList());
        }
        dbQuery.close();
        if (tables.size() > 0) {
            return tables.get(0);
        }
        return null;
    }

    @Override
    public List<DbName> getDatabaseListByDatasourceId(Long id) {
        AssetsDatasourceRespDTO datasource = this.getDatasourceById(id);
        if (datasource == null) {
            throw new DataQueryException("");
        }
        DbQueryProperty baseProperty = new DbQueryProperty(datasource.getDatasourceType(), datasource.getIp(), datasource.getPort(), datasource.getDatasourceConfig());

// 1. 先获取顶层数据库
        List<DbName> dbNames;
        DbQuery rootQuery = DataSourceFactory.createDbQuery(baseProperty);
        try {
            if (!rootQuery.valid()) {
                throw new DataQueryException("数据库连接失败");
            }
            dbNames = rootQuery.getDbNames(null);
        } finally {
            rootQuery.close();
        }
        if (CollectionUtils.isEmpty(dbNames)) {
            return dbNames;
        }
// 单层结构，直接返回
        if (dbNames.get(0).getLevel() == 1 && dbNames.get(0).getTotalLevels() == 1) {
            return dbNames;
        }
// 2. 逐个加载下级
        for (DbName dbName : dbNames) {
// Kingbase / PostgreSQL 需要切换 dbName
            DbQueryProperty childProperty = baseProperty;
            if (DbType.KINGBASE8.getDb().equals(baseProperty.getDbType()) || DbType.POSTGRE_SQL.getDb().equals(baseProperty.getDbType())) {
                childProperty = baseProperty.copy();
                childProperty.setDbName(dbName.getDbName());
            }
            DbQuery childQuery = DataSourceFactory.createDbQuery(childProperty);
            try {
                if (!childQuery.valid()) {
// 不影响整体，直接跳过当前节点
                    continue;
                }
                List<DbName> children = childQuery.getDbNames(dbName);
                dbName.setChildren(children);
            } catch (Exception e) {
// 可选：记录日志
// log.warn("获取数据库 {} 子级失败", dbName.getDbName(), e);
            } finally {
                childQuery.close();
            }
        }
        return dbNames;
    }

    @Override
    public List<AssetsDatasourceRespDTO> getDatabaseListByIds(List<Long> ids) {
        List<AssetsDatasourceDO> AssetsDatasourceDOS = AssetsDatasourceMapper.selectBatchIds(ids);
        return BeanUtils.toBean(AssetsDatasourceDOS, AssetsDatasourceRespDTO.class);
    }

    private List<AssetsDiscoveryTableDO> fetchDiscoveryTableList(AssetsDiscoveryTaskRespVO AssetsDiscoveryTaskDO, Long AssetsDiscoveryTaskLog) {
        IAssetsDiscoveryLogBodyService.taskLogAppend(AssetsDiscoveryTaskLog, "ID" + AssetsDiscoveryTaskDO.getId());
        AssetsDiscoveryTablePageReqVO AssetsDiscoveryTablePageReqVO = new AssetsDiscoveryTablePageReqVO();
        AssetsDiscoveryTablePageReqVO.setTaskId(AssetsDiscoveryTaskDO.getId());
        List<AssetsDiscoveryTableDO> result = IAssetsDiscoveryTableService.getDaDiscoveryTableList(AssetsDiscoveryTablePageReqVO);
        IAssetsDiscoveryLogBodyService.taskLogAppend(AssetsDiscoveryTaskLog, "" + (result != null ? result.size() : 0));
        return result;
    }

    private List<AssetsDiscoveryColumnDO> fetchDaDiscoveryColumnDOList(AssetsDiscoveryTableDO matchedTable, Long AssetsDiscoveryTaskLog) {
        IAssetsDiscoveryLogBodyService.taskLogAppend(AssetsDiscoveryTaskLog, "ID" + matchedTable.getId() + "ID" + matchedTable.getTaskId());
        AssetsDiscoveryColumnPageReqVO AssetsDiscoveryTablePageReqVO = new AssetsDiscoveryColumnPageReqVO();
        AssetsDiscoveryTablePageReqVO.setTaskId(matchedTable.getTaskId());
        AssetsDiscoveryTablePageReqVO.setTableId(matchedTable.getId());
        List<AssetsDiscoveryColumnDO> result = IAssetsDiscoveryColumnService.getDaDiscoveryColumnList(AssetsDiscoveryTablePageReqVO);
        IAssetsDiscoveryLogBodyService.taskLogAppend(AssetsDiscoveryTaskLog, "" + (result != null ? result.size() : 0));
        return result;
    }

    private List<AssetsDiscoveryTableDO> mapToMetadataTableList(List<DbTable> tables, Long taskId) {
        return tables.stream().map(table -> {
            AssetsDiscoveryTableDO metadataTable = new AssetsDiscoveryTableDO();
            metadataTable.setTaskId(taskId);
            metadataTable.setTableName(table.getTableName());
            metadataTable.setTableComment(table.getTableComment());
            return metadataTable;
        }).collect(Collectors.toList());
    }

    private void updateTableDataCount(DbQuery dbQuery, AssetsDiscoveryTableDO table, int fieldCount) {
        int dataCount = dbQuery.countNew(table.getTableName(), new HashMap<>());
        table.setDataCount((long) dataCount);
        table.setFieldCount((long) fieldCount);
        table.setCreateBy("");
        table.setCreatorId(1L);
    }

    private int updateTableStatus(AssetsDiscoveryTableDO matchedTable, AssetsDiscoveryTableDO table, boolean modifiedTablesBoolean, Long AssetsDiscoveryTaskLog) {
        if (modifiedTablesBoolean) {
//1:新增，2:修改，3:删除，4:无变化
            table.setChangeFlag("2");
            IAssetsDiscoveryLogBodyService.taskLogAppend(AssetsDiscoveryTaskLog, "更新表状态为修改：" + matchedTable.getTableName());
            IAssetsDiscoveryTableService.updateDaDiscoveryTable(table);
            IAssetsDiscoveryLogBodyService.taskLogAppend(AssetsDiscoveryTaskLog, "更新完毕");
            return 1;
        } else {
//1:新增，2:修改，3:删除，4:无变化
            matchedTable.setChangeFlag("4");
            IAssetsDiscoveryLogBodyService.taskLogAppend(AssetsDiscoveryTaskLog, "更新表状态为无变化：" + matchedTable.getTableName());
            IAssetsDiscoveryTableService.updateDaDiscoveryTable(matchedTable);
            IAssetsDiscoveryLogBodyService.taskLogAppend(AssetsDiscoveryTaskLog, "更新完毕");
            return 0;
        }
    }

    private void saveNewTable(AssetsDiscoveryTableDO table, List<DbColumn> columns, DbQuery dbQuery, DbQueryProperty dbQueryProperty, Long AssetsDiscoveryTaskLog) {
        IAssetsDiscoveryLogBodyService.taskLogAppend(AssetsDiscoveryTaskLog, "保存新表：" + table.getTableName());
        updateTableDataCount(dbQuery, table, columns.size());

// 1:新增，2:修改，3:删除，4:无变化
        table.setChangeFlag("1");
        IAssetsDiscoveryTableService.createDaDiscoveryTable(table);
        if (CollUtil.isNotEmpty(columns)) {
            List<AssetsDiscoveryColumnDO> metadataColumnEntityList = columns.stream().map(column -> new AssetsDiscoveryColumnDO(table.getTaskId(), table.getId(), column)).collect(Collectors.toList());
            metadataColumnEntityList.forEach(IAssetsDiscoveryColumnService::createDaDiscoveryColumn);
        }
        IAssetsDiscoveryLogBodyService.taskLogAppend(AssetsDiscoveryTaskLog, "保存完毕");
    }

    public AssetsDiscoveryTableDO findMatchedTable(AssetsDiscoveryTableDO table, List<AssetsDiscoveryTableDO> AssetsDiscoveryTableDOList) {
        return AssetsDiscoveryTableDOList.stream().filter(existingTable -> existingTable.getTableName().equals(table.getTableName()) &&
                existingTable.getTaskId().equals(table.getTaskId())).findFirst().orElse(null);

// 如果没有匹配到，返回null
    }

    private boolean isTableCommentModified(AssetsDiscoveryTableDO table, AssetsDiscoveryTableDO matchedTable) {
        return !StringUtils.equals(table.getTableComment(), matchedTable.getTableComment());
    }

    private List<AssetsDiscoveryColumnDO> generateMetadataColumnList(List<DbColumn> columns, AssetsDiscoveryTableDO matchedTable) {
        if (CollUtil.isEmpty(columns)) {
            return new ArrayList<>();
        }
        return columns.stream().map(column -> new AssetsDiscoveryColumnDO(matchedTable.getTaskId(), matchedTable.getId(), column)).collect(Collectors.toList());
    }

    private boolean compareColumnsAndUpdate(List<AssetsDiscoveryColumnDO> metadataColumnEntityList, List<AssetsDiscoveryColumnDO> discoveryColumnDOList) {
        boolean modifiedTablesBoolean = false;
        for (AssetsDiscoveryColumnDO column : metadataColumnEntityList) {
            AssetsDiscoveryColumnDO matchedColumn = findMatchedColumn(column, discoveryColumnDOList);
            if (matchedColumn == null) {
                modifiedTablesBoolean = true;
                IAssetsDiscoveryColumnService.createDaDiscoveryColumn(column);
            } else if (!column.isEqual(matchedColumn)) {
                modifiedTablesBoolean = true;
                IAssetsDiscoveryColumnService.updateDaDiscoveryColumn(column);
            }
        }
        return modifiedTablesBoolean;
    }

    public AssetsDiscoveryColumnDO findMatchedColumn(AssetsDiscoveryColumnDO table, List<AssetsDiscoveryColumnDO> AssetsDiscoveryTableDOList) {
        return AssetsDiscoveryTableDOList.stream().filter(existingTable -> StringUtils.equals(existingTable.getColumnName(), table.getColumnName())).findFirst().orElse(null);

// 如果没有匹配到，返回null
    }

    private boolean deleteUnmatchedColumns(List<AssetsDiscoveryColumnDO> discoveryColumnDOList, List<AssetsDiscoveryColumnDO> metadataColumnEntityList) {
        List<AssetsDiscoveryColumnDO> notInMetadataTable = findNotInDaDiscoveryColumn(discoveryColumnDOList, metadataColumnEntityList);
        if (CollectionUtils.isEmpty(notInMetadataTable)) {
            return false;
        }
        Collection<Long> idList = notInMetadataTable.stream().map(AssetsDiscoveryColumnDO::getId).collect(Collectors.toList());
        IAssetsDiscoveryColumnService.removeDaDiscoveryColumn(idList);
        return true;
    }

    public List<AssetsDiscoveryColumnDO> findNotInDaDiscoveryColumn(List<AssetsDiscoveryColumnDO> discoveryColumnDOList, List<AssetsDiscoveryColumnDO> metadataTableEntityList) {
        return discoveryColumnDOList.stream().filter(table -> metadataTableEntityList.stream().noneMatch(existingTable -> {
            return StringUtils.equals(existingTable.getColumnName(), table.getColumnName());
        })).collect(Collectors.toList());

// 返回daDiscoveryTableDOList中在metadataTableEntityList中不存在的table
    }

    private int updateExistingTable(DbQuery dbQuery, AssetsDiscoveryTableDO matchedTable, AssetsDiscoveryTableDO table, List<DbColumn> columns, Long AssetsDiscoveryTaskLog) {
        IAssetsDiscoveryLogBodyService.taskLogAppend(AssetsDiscoveryTaskLog, "更新表：" + table.getTableName());
        boolean modifiedTablesBoolean = false;

// 查询表存的快照字段结构
        List<AssetsDiscoveryColumnDO> discoveryColumnDOList = this.fetchDaDiscoveryColumnDOList(matchedTable, AssetsDiscoveryTaskLog);
        discoveryColumnDOList = discoveryColumnDOList == null ? new ArrayList<>() : discoveryColumnDOList;
        if (isTableCommentModified(table, matchedTable)) {
            modifiedTablesBoolean = true;
        }
        List<AssetsDiscoveryColumnDO> metadataColumnEntityList = generateMetadataColumnList(columns, matchedTable);
        modifiedTablesBoolean |= compareColumnsAndUpdate(metadataColumnEntityList, discoveryColumnDOList);
        modifiedTablesBoolean |= deleteUnmatchedColumns(discoveryColumnDOList, metadataColumnEntityList);
        updateTableDataCount(dbQuery, table, columns.size());
        return updateTableStatus(matchedTable, table, modifiedTablesBoolean, AssetsDiscoveryTaskLog);
    }

    private Map<String, Object> logSchemaModifications(DbQueryProperty dbQueryProperty, DbQuery dbQuery, AssetsDiscoveryTaskRespVO AssetsDiscoveryTaskById, Long AssetsDiscoveryTaskLog) {
        IAssetsDiscoveryLogBodyService.taskLogAppend(AssetsDiscoveryTaskLog, "开始执行模式修改操作，任务ID：" + AssetsDiscoveryTaskById.getId());
        int newTables = 0;
        int modifiedTables = 0;
        int deletedTables = 0;
        int totalTables = 0;
        List<AssetsDiscoveryTableDO> AssetsDiscoveryTableDOList = this.fetchDiscoveryTableList(AssetsDiscoveryTaskById, AssetsDiscoveryTaskLog);
        AssetsDiscoveryTableDOList = AssetsDiscoveryTableDOList == null ? new ArrayList<>() : AssetsDiscoveryTableDOList;
        List<DbTable> tables = dbQuery.getTables(dbQueryProperty);
        List<AssetsDiscoveryTableDO> metadataTableEntityList = new ArrayList<>();
        if (CollUtil.isNotEmpty(tables)) {
            totalTables = tables.size();
            IAssetsDiscoveryLogBodyService.taskLogAppend(AssetsDiscoveryTaskLog, "从数据源中，实时获取表列数量信息：" + totalTables);
            metadataTableEntityList = mapToMetadataTableList(tables, AssetsDiscoveryTaskById.getId());
            if (CollUtil.isNotEmpty(metadataTableEntityList)) {
                for (AssetsDiscoveryTableDO table : metadataTableEntityList) {
                    AssetsDiscoveryTableDO matchedTable = findMatchedTable(table, AssetsDiscoveryTableDOList);
                    IAssetsDiscoveryLogBodyService.taskLogAppend(AssetsDiscoveryTaskLog, "正在处理表：" + table.getTableName());
                    List<DbColumn> columns = dbQuery.getTableColumns(dbQueryProperty, table.getTableName());
                    columns = columns == null ? new ArrayList<>() : columns;
                    IAssetsDiscoveryLogBodyService.taskLogAppend(AssetsDiscoveryTaskLog, "从数据源中，实时获取列数量信息：" + columns.size());
                    if (matchedTable == null) {
                        newTables++;
                        IAssetsDiscoveryLogBodyService.taskLogAppend(AssetsDiscoveryTaskLog, "新表发现，表：" + table.getTableName() + "，开始保存");
                        saveNewTable(table, columns, dbQuery, dbQueryProperty, AssetsDiscoveryTaskLog);
                    } else {
                        IAssetsDiscoveryLogBodyService.taskLogAppend(AssetsDiscoveryTaskLog, "查看表[ " + table.getTableName() + " ]库中配置信息");

// 是否忽略;0:否，1：是
                        String ignoreFlag = matchedTable.getIgnoreFlag();
                        IAssetsDiscoveryLogBodyService.taskLogAppend(AssetsDiscoveryTaskLog, "查看表[ " + table.getTableName() + " ]库中配置信息,发现配置ignoreFlag为：" + ignoreFlag);
                        if (StringUtils.equals("1", ignoreFlag)) {
                            IAssetsDiscoveryLogBodyService.taskLogAppend(AssetsDiscoveryTaskLog, "查看表[ " + table.getTableName() + " ]库中配置信息,发现配置为：忽略。该表结束扫描！");
                            continue;
                        }
                        table.setId(matchedTable.getId());
                        modifiedTables += updateExistingTable(dbQuery, matchedTable, table, columns, AssetsDiscoveryTaskLog);
                        IAssetsDiscoveryLogBodyService.taskLogAppend(AssetsDiscoveryTaskLog, "已存在表更新，表：" + table.getTableName());
                    }
                }
            }
        }
        deletedTables = deleteUnmatchedTables(AssetsDiscoveryTableDOList, metadataTableEntityList, AssetsDiscoveryTaskLog);
        String executionTime = DateUtils.getExecutionTime();
        IAssetsDiscoveryLogBodyService.taskLogAppend(AssetsDiscoveryTaskLog, "模式修改操作完成，总表数：" + totalTables + "。其中，新增表数：" + newTables + "，修改表数：" + modifiedTables + "，删除表数：" + deletedTables);
        Map<String, Object> map = new HashMap<>();
        map.put("taskName", AssetsDiscoveryTaskById.getName());
        map.put("executionTime", executionTime);
        map.put("totalTables", totalTables);
        map.put("newTables", newTables);
        map.put("modifiedTables", modifiedTables);
        map.put("deletedTables", deletedTables);
        AssetsDiscoveryTaskById.setLastTableCount((long) (newTables + modifiedTables + deletedTables));
        return map;
    }

    public List<AssetsDiscoveryTableDO> findNotInMetadataTable(List<AssetsDiscoveryTableDO> AssetsDiscoveryTableDOList, List<AssetsDiscoveryTableDO> metadataTableEntityList) {
        return AssetsDiscoveryTableDOList.stream().filter(table -> metadataTableEntityList.stream().noneMatch(existingTable -> existingTable.getTableName().equals(table.getTableName()) &&
                existingTable.getTaskId().equals(table.getTaskId()))).collect(Collectors.toList());

// 返回daDiscoveryTableDOList中在metadataTableEntityList中不存在的table
    }

    private int deleteUnmatchedTables(List<AssetsDiscoveryTableDO> AssetsDiscoveryTableDOList, List<AssetsDiscoveryTableDO> metadataTableEntityList, Long AssetsDiscoveryTaskLog) {
        List<AssetsDiscoveryTableDO> notInMetadataTable = findNotInMetadataTable(AssetsDiscoveryTableDOList, metadataTableEntityList);
        if (CollectionUtils.isEmpty(notInMetadataTable)) return 0;
        for (AssetsDiscoveryTableDO AssetsDiscoveryTableDO : notInMetadataTable) {
            AssetsDiscoveryTableDO.setUpdateBy("超级管理员");
            AssetsDiscoveryTableDO.setUpdatorId(1L);

// 1:新增，2:修改，3:删除，4:无变化
            AssetsDiscoveryTableDO.setChangeFlag("3");
            IAssetsDiscoveryLogBodyService.taskLogAppend(AssetsDiscoveryTaskLog, "删除未匹配表：" + AssetsDiscoveryTableDO.getTableName());
            IAssetsDiscoveryTableService.updateDaDiscoveryTable(AssetsDiscoveryTableDO);
        }
        IAssetsDiscoveryLogBodyService.taskLogAppend(AssetsDiscoveryTaskLog, "删除完毕");
        return notInMetadataTable.size();
    }

    private Map<String, Object> runJobTableSchemaUpdates(AssetsDiscoveryTaskRespVO AssetsDiscoveryTaskById, Long AssetsDiscoveryTaskLog) {
        IAssetsDiscoveryLogBodyService.taskLogAppend(AssetsDiscoveryTaskLog, "任务执行-根据数据源编号，获取发现任务的 数据源详细信息");
        AssetsDatasourceDO AssetsDatasourceBy = this.getDatasourceDOById(AssetsDiscoveryTaskById.getDatasourceId());
        if (AssetsDatasourceBy == null) {
            throw new DataQueryException("任务执行-根据数据源编号，获取发现任务的 数据源详情信息查询失败！");
        }
        IAssetsDiscoveryLogBodyService.taskLogAppend(AssetsDiscoveryTaskLog, "任务执行-根据数据源编号，获取发现任务的 数据源详细信息成功");
        IAssetsDiscoveryLogBodyService.taskLogAppend(AssetsDiscoveryTaskLog, "任务执行-根据数据源链接信息，建立实时数据源链接");
        DbQueryProperty dbQueryProperty = new DbQueryProperty(AssetsDatasourceBy.getDatasourceType(), AssetsDatasourceBy.getIp(), AssetsDatasourceBy.getPort(), AssetsDatasourceBy.getDatasourceConfig());
        DbQuery dbQuery = DataSourceFactory.createDbQuery(dbQueryProperty);
        if (!dbQuery.valid()) {
            throw new DataQueryException("任务执行-根据数据源链接信息，建立实时数据源链接 失败！");
        }
        IAssetsDiscoveryLogBodyService.taskLogAppend(AssetsDiscoveryTaskLog, "任务执行-根据数据源链接信息，建立实时数据源链接 成功");
        IAssetsDiscoveryLogBodyService.taskLogAppend(AssetsDiscoveryTaskLog, "任务执行-根据数据源链接，开始进入获取实时库中信息方法");
        try {
            Map<String, Object> map = logSchemaModifications(dbQueryProperty, dbQuery, AssetsDiscoveryTaskById, AssetsDiscoveryTaskLog);
            IAssetsDiscoveryLogBodyService.taskLogAppend(AssetsDiscoveryTaskLog, "任务执行-根据数据源链接，获取实时库中信息方法结束");
            IAssetsDiscoveryLogBodyService.taskLogAppend(AssetsDiscoveryTaskLog, "任务执行-信息如下 map:" + map.toString());
            IAssetsDiscoveryTaskService.updateDaDiscoveryTask(AssetsDiscoveryTaskById);
            return map;
        } catch (Exception e) {
            throw e;
        } finally {
            dbQuery.close();
        }
    }

    /***     * @param id     */

    @Override
    public void detectTableSchemaUpdates(Long id) {
        String key = "detectTableSchemaUpdates-" + id;
        String status = redisService.get(key);
        if (StringUtils.isEmpty(status) && StringUtils.equals("1", status)) {
            throw new RuntimeException("");
        }
        AssetsDiscoveryTaskRespVO AssetsDiscoveryTaskById = IAssetsDiscoveryTaskService.getDaDiscoveryTaskById(id);
        if (AssetsDiscoveryTaskById == null) {
            throw new DataQueryException("- !");
        }
        redisService.set(key, "1", 1200);

//创建日志记录表
        AssetsDiscoveryTaskLogSaveReqVO createReqVO = new AssetsDiscoveryTaskLogSaveReqVO();
        Date executionDate = DateUtils.getExecutionDate();
        createReqVO.setStartTime(executionDate);
        createReqVO.populateFromTask(AssetsDiscoveryTaskById);
        Long AssetsDiscoveryTaskLog = IAssetsDiscoveryTaskLogService.createDaDiscoveryTaskLog(createReqVO);
        createReqVO.setId(AssetsDiscoveryTaskLog);
        IAssetsDiscoveryLogBodyService.taskLogAppend(AssetsDiscoveryTaskLog, "任务执行-根据发现任务编号，获取发现任务详细信息成功");
        IAssetsDiscoveryLogBodyService.taskLogAppend(AssetsDiscoveryTaskLog, "任务开始执行");
        try {
            AssetsDiscoveryTaskById.setLastExecuteTime(executionDate);

// 
            Map<String, Object> map = runJobTableSchemaUpdates(AssetsDiscoveryTaskById, AssetsDiscoveryTaskLog);
            int newTables = MapUtils.getIntValue(map, "newTables");
            int modifiedTables = MapUtils.getIntValue(map, "modifiedTables");
            int deletedTables = MapUtils.getIntValue(map, "deletedTables");
            createReqVO.setNewTableCount((long) newTables);
            createReqVO.setModifiedTableCount((long) modifiedTables);
            createReqVO.setDeletedTableCount((long) deletedTables);
            IAssetsDiscoveryLogBodyService.taskLogAppend(AssetsDiscoveryTaskLog, "任务执行-根据任务执行信息，开始对本次任务发放站内信");
            iSysMessageService.sendDbChangeMessage(AssetsDiscoveryTaskById.getContactId(), map);
            IAssetsDiscoveryLogBodyService.taskLogAppend(AssetsDiscoveryTaskLog, "任务执行-根据任务执行信息，对本次任务站内信发放 完毕");
            createReqVO.setStatus("2");
        } catch (Exception e) {
            IAssetsDiscoveryLogBodyService.taskLogAppend(AssetsDiscoveryTaskLog, "任务失败");
            createReqVO.setStatus("3");
            IAssetsDiscoveryLogBodyService.taskLogAppend(AssetsDiscoveryTaskLog, e.getMessage().toString());
            redisService.set(key, "3", 300);
        } finally {
            IAssetsDiscoveryLogBodyService.taskLogAppend(AssetsDiscoveryTaskLog, "FINALIZE_SESSION");
            createReqVO.setPath("");
            IAssetsDiscoveryLogBodyService.taskLogAppend(AssetsDiscoveryTaskLog, "任务结束");
            createReqVO.setEndTime(DateUtils.getExecutionDate());
            IAssetsDiscoveryTaskLogService.updateDaDiscoveryTaskLog(createReqVO);
            redisService.set(key, "2", 300);
        }
    }
}