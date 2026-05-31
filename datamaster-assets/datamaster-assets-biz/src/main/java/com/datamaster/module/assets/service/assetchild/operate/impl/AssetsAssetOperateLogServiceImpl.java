package com.datamaster.module.assets.service.assetchild.operate.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.database.DataSourceFactory;
import com.datamaster.common.database.DbDialect;
import com.datamaster.common.database.DbQuery;
import com.datamaster.common.database.DialectFactory;
import com.datamaster.common.database.constants.DbDataType;
import com.datamaster.common.database.constants.DbQueryProperty;
import com.datamaster.common.database.constants.DbType;
import com.datamaster.common.database.core.DbColumn;
import com.datamaster.common.database.exception.DataQueryException;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.DateUtils;
import com.datamaster.common.utils.JSONUtils;
import com.datamaster.common.utils.MD5Util;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.module.assets.controller.admin.asset.vo.AssetsAssetRespVO;
import com.datamaster.module.assets.controller.admin.assetchild.operate.vo.AssetsAssetOperateLogPageReqVO;
import com.datamaster.module.assets.controller.admin.assetchild.operate.vo.AssetsAssetOperateLogRespVO;
import com.datamaster.module.assets.controller.admin.assetchild.operate.vo.AssetsAssetOperateLogSaveReqVO;
import com.datamaster.module.assets.controller.admin.datasource.vo.AssetsDatasourceRespVO;
import com.datamaster.module.assets.dal.dataobject.assetchild.operate.AssetsAssetOperateLogDO;
import com.datamaster.module.assets.dal.mapper.assetchild.operate.AssetsAssetOperateLogMapper;
import com.datamaster.module.assets.service.asset.IAssetsAssetService;
import com.datamaster.module.assets.service.assetchild.operate.IAssetsAssetOperateLogService;
import com.datamaster.module.assets.service.datasource.IAssetsDatasourceService;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * Service
 *
 * @author qdata
 * @date 2025-05-09
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class AssetsAssetOperateLogServiceImpl extends ServiceImpl<AssetsAssetOperateLogMapper, AssetsAssetOperateLogDO> implements IAssetsAssetOperateLogService {

    private final static String sql_INSERT = "INSERT INTO {tableName} ({columns}) VALUES ({values})";
    private final static String sql_UPDATE = "UPDATE {tableName} SET {setValue} WHERE {where}";
    private final static String sql_DELETE = "DELETE FROM {tableName} WHERE {where}";

    @Resource
    private AssetsAssetOperateLogMapper AssetsAssetOperateLogMapper;

    @Autowired
    private DataSourceFactory DataSourceFactory;

    @Autowired
    @Lazy
    private IAssetsAssetService IAssetsAssetService;

    @Autowired
    @Lazy
    private IAssetsDatasourceService IAssetsDatasourceService;

    @Override
    public PageResult<AssetsAssetOperateLogDO> getDaAssetOperateLogPage(AssetsAssetOperateLogPageReqVO pageReqVO) {
        return AssetsAssetOperateLogMapper.selectPage(pageReqVO);
    }

    @Override
    public PageResult<AssetsAssetOperateLogDO> queryDaAssetOperateLogPage(AssetsAssetOperateLogPageReqVO AssetsAssetOperateLog) {

        Map<String, Object> after = JSONUtils.convertTaskDefinitionJsonMap(AssetsAssetOperateLog.getUpdateBefore());
        Map<String, Object> keys = JSONUtils.convertTaskDefinitionJsonMap(AssetsAssetOperateLog.getFieldNames());
        List<String> whereCols = JSONUtils.splitListByString(keys.get("commentKeyList"));
        AssetsAssetOperateLogSaveReqVO AssetsAssetOperateLogSaveReqVO = new AssetsAssetOperateLogSaveReqVO();
        fillUpdateWhereMd5(AssetsAssetOperateLogSaveReqVO, after, whereCols);

        AssetsAssetOperateLog.setUpdateWhereMd5(AssetsAssetOperateLogSaveReqVO.getUpdateWhereMd5());

        return AssetsAssetOperateLogMapper.selectPage(AssetsAssetOperateLog);
    }

    @Override
    public int removeDaAssetOperateLog(Collection<Long> idList) {
        // 批量删除数据资产操作记录
        return AssetsAssetOperateLogMapper.deleteBatchIds(idList);
    }

    @Override
    public AssetsAssetOperateLogDO getDaAssetOperateLogById(Long id) {
        return AssetsAssetOperateLogMapper.selectById(id);
    }

    @Override
    public List<AssetsAssetOperateLogDO> getDaAssetOperateLogList() {
        return AssetsAssetOperateLogMapper.selectList();
    }

    @Override
    public Map<Long, AssetsAssetOperateLogDO> getDaAssetOperateLogMap() {
        List<AssetsAssetOperateLogDO> AssetsAssetOperateLogList = AssetsAssetOperateLogMapper.selectList();
        return AssetsAssetOperateLogList.stream()
                .collect(Collectors.toMap(
                        AssetsAssetOperateLogDO::getId,
                        AssetsAssetOperateLogDO -> AssetsAssetOperateLogDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing
                ));
    }

    /**
     *
     *
     * @param importExcelList
     * @param isUpdateSupport
     * @param operName
     * @return
     */
    @Override
    public String importDaAssetOperateLog(List<AssetsAssetOperateLogRespVO> importExcelList, boolean isUpdateSupport, String operName) {
        if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
            throw new ServiceException("");
        }

        int successNum = 0;
        int failureNum = 0;
        List<String> successMessages = new ArrayList<>();
        List<String> failureMessages = new ArrayList<>();

        for (AssetsAssetOperateLogRespVO respVO : importExcelList) {
            try {
                AssetsAssetOperateLogDO AssetsAssetOperateLogDO = BeanUtils.toBean(respVO, AssetsAssetOperateLogDO.class);
                Long AssetsAssetOperateLogId = respVO.getId();
                if (isUpdateSupport) {
                    if (AssetsAssetOperateLogId != null) {
                        AssetsAssetOperateLogDO existingDaAssetOperateLog = AssetsAssetOperateLogMapper.selectById(AssetsAssetOperateLogId);
                        if (existingDaAssetOperateLog != null) {
                            AssetsAssetOperateLogMapper.updateById(AssetsAssetOperateLogDO);
                            successNum++;
                            successMessages.add("ID " + AssetsAssetOperateLogId + " ");
                        } else {
                            failureNum++;
                            failureMessages.add("ID " + AssetsAssetOperateLogId + " ");
                        }
                    } else {
                        failureNum++;
                        failureMessages.add("ID");
                    }
                } else {
                    QueryWrapper<AssetsAssetOperateLogDO> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("id", AssetsAssetOperateLogId);
                    AssetsAssetOperateLogDO existingDaAssetOperateLog = AssetsAssetOperateLogMapper.selectOne(queryWrapper);
                    if (existingDaAssetOperateLog == null) {
                        AssetsAssetOperateLogMapper.insert(AssetsAssetOperateLogDO);
                        successNum++;
                        successMessages.add("ID " + AssetsAssetOperateLogId + " ");
                    } else {
                        failureNum++;
                        failureMessages.add("ID " + AssetsAssetOperateLogId + " ");
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
    public void rollBack(Long id) {
        AssetsAssetOperateLogDO AssetsAssetOperateLogById = this.getDaAssetOperateLogById(id);
        if (AssetsAssetOperateLogById == null || AssetsAssetOperateLogById.getDelFlag()) {
            throw new AssetOperateException("");
        }
        //判断状态 状态;1:执行中  2:失败  3:成功   4:回滚失败  5:回滚成功
        String status = AssetsAssetOperateLogById.getStatus();
        if (StringUtils.equals("1", status)
                || StringUtils.equals("2", status)
                || StringUtils.equals("5", status)) {
            throw new AssetOperateException("");
        }
        String operateType = AssetsAssetOperateLogById.getOperateType();
        AssetsAssetOperateLogSaveReqVO bean = BeanUtils.toBean(AssetsAssetOperateLogById, AssetsAssetOperateLogSaveReqVO.class);
        this.applyOperateTypeLogic(bean, operateType);
        this.updateDaAssetOperateLog(bean);
    }

    /**
     * @param bean         VO
     * @param operateType "1""2""3""4"
     * @return  VO
     */
    public static AssetsAssetOperateLogSaveReqVO applyOperateTypeLogic(
            AssetsAssetOperateLogSaveReqVO bean,
            String operateType) {

        bean.setStatus("-1");
        bean.setOperateType(mapOperateType(operateType));

        if ("2".equals(operateType)) {
            String before = bean.getUpdateBefore();
            bean.setUpdateBefore(bean.getUpdateAfter());
            bean.setUpdateAfter(before);
        }
        // 对于 "1"、"3"、"4" 不做额外处理，直接返回原 bean
        return bean;
    }

    /**
     *  operateType
     *  (1) -> "3"
     *  (2) -> "2"
     *  (3) -> "1"
     *       -> ""
     */
    public static String mapOperateType(String operateType) {
        if (operateType == null) {
            return "";
        }
        switch (operateType) {
            case "1": // 新增
                return "3";
            case "2": // 修改
                return "2";
            case "3": // 删除
                return "1";
            default:  // 导入(4) 或未知类型
                return "0";
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateDaAssetOperateLog(AssetsAssetOperateLogSaveReqVO updateReqVO) {
        // 相关校验
        // 1. 校验资产和数据源
        AssetsAssetRespVO asset = IAssetsAssetService.getDaAssetByIdSimple(updateReqVO.getAssetId());
        if (asset == null || asset.getDelFlag()) {
            throw new AssetOperateException("");
        }
        AssetsDatasourceRespVO ds = IAssetsDatasourceService.getDaDatasourceByIdSimple(updateReqVO.getDatasourceId());
        if (ds == null) {
            throw new AssetOperateException("");
        }

        // 2. 分发到具体操作
        String type = StringUtils.trimToNull(updateReqVO.getOperateType());
        if (type == null) {
            throw new AssetOperateException("");
        }
        PreContext ctx = prepareContext(ds, updateReqVO.getTableName());
        handlers.getOrDefault(type, (r, c) -> {
            throw new AssetOperateException(": " + type);
        }).accept(updateReqVO, ctx);

        // 更新数据资产操作记录
        AssetsAssetOperateLogDO updateObj = new AssetsAssetOperateLogDO();
        updateObj.setId(updateReqVO.getId());
        updateObj.setStatus(updateReqVO.getStatus());
        updateObj.setExecuteTime(updateReqVO.getExecuteTime());
        return AssetsAssetOperateLogMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createDaAssetOperateLog(AssetsAssetOperateLogSaveReqVO reqVO) {
        // 1. 校验资产和数据源
        AssetsAssetRespVO asset = IAssetsAssetService.getDaAssetByIdSimple(reqVO.getAssetId());
        if (asset == null || asset.getDelFlag()) {
            throw new AssetOperateException("");
        }
        AssetsDatasourceRespVO ds = IAssetsDatasourceService.getDaDatasourceByIdSimple(reqVO.getDatasourceId());
        if (ds == null) {
            throw new AssetOperateException("");
        }

        // 2. 分发到具体操作
        String type = StringUtils.trimToNull(reqVO.getOperateType());
        if (type == null) {
            throw new AssetOperateException("");
        }
        PreContext ctx = prepareContext(ds, reqVO.getTableName());
        handlers.getOrDefault(type, (r, c) -> {
            throw new AssetOperateException(": " + type);
        }).accept(reqVO, ctx);

        // 3. 写入日志
        AssetsAssetOperateLogDO logDo = BeanUtils.toBean(reqVO, AssetsAssetOperateLogDO.class);
        AssetsAssetOperateLogMapper.insert(logDo);
        return logDo.getId();
    }

    /**
     *
     */
    private PreContext prepareContext(AssetsDatasourceRespVO ds, String tableName) {
        if (StringUtils.isBlank(tableName)) {
            throw new AssetOperateException("");
        }
        DbQueryProperty prop = new DbQueryProperty(
                ds.getDatasourceType(), ds.getIp(), ds.getPort(), ds.getDatasourceConfig());
        DbQuery query = DataSourceFactory.createDbQuery(prop);
        if (!query.valid()) {
            throw new DataQueryException("");
        }
        if (query.generateCheckTableExistsSQL(prop, tableName) == 0) {
            throw new DataQueryException("");
        }
        List<DbColumn> cols = query.getTableColumns(prop, tableName);
        DbDialect dbDialect = DialectFactory.getDialect(DbType.getDbType(prop.getDbType()));
        return new PreContext(query, prop, cols, dbDialect.getTableName(prop, tableName));
    }

    private final Map<String, BiConsumer<AssetsAssetOperateLogSaveReqVO, PreContext>> handlers = new HashMap<>();

    @PostConstruct
    private void init() {
        handlers.put("1", this::doAdd);
        handlers.put("2", this::doUpdate);
        handlers.put("3", this::doDelete);
        handlers.put("4", this::doImport);
    }

    /**
     *  keys  commentKeyList after
     *  Map JSON  MD5  req
     *
     * @param req        updateWhereMd5
     * @param after      Map
     * @param whereCols  commentKeyList  Map
     */
    public static void fillUpdateWhereMd5(AssetsAssetOperateLogSaveReqVO req,
                                          Map<String, Object> after,
                                          List<String> whereCols) {
        // 2. 按顺序从 after 中取值
        Map<String, Object> whereMap = new LinkedHashMap<>(whereCols.size());
        for (String col : whereCols) {
            if (after.containsKey(col)) {
                whereMap.put(col, after.get(col));
            }
        }
        // 3. 序列化成 JSON
        String json = JSON.toJSONString(whereMap);
        // 4. 计算 MD5 并设置
        String md5 = null;
        try {
            md5 = MD5Util.getInstance().encode(json);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        req.setUpdateWhereMd5(md5);
    }

    /**
     *
     */
    private void doAdd(AssetsAssetOperateLogSaveReqVO req, PreContext ctx) {
        Map<String, Object> after = JSONUtils.convertTaskDefinitionJsonMap(req.getUpdateAfter());

        Map<String, Object> keys = JSONUtils.convertTaskDefinitionJsonMap(req.getFieldNames());
        List<String> whereCols = JSONUtils.splitListByString(keys.get("commentKeyList"));
        fillUpdateWhereMd5(req, after, whereCols);

        StringJoiner colsJs = new StringJoiner(","), valsJs = new StringJoiner(",");
        after.forEach((key, val) -> {
            DbColumn col = findColumn(key, ctx.columns);
            colsJs.add(col.getColName());
            valsJs.add(packFormatValue(val, col.getDataType(), ctx.prop));
        });

        String sql = sql_INSERT
                .replace("{tableName}", ctx.fullTable)
                .replace("{columns}", colsJs.toString())
                .replace("{values}", valsJs.toString());
        log.info("ADD SQL: {}", sql);

        req.setExecuteTime(DateUtils.getExecutionDate());
        int cnt = ctx.query.update(sql);
        if (StringUtils.isNotEmpty(req.getStatus()) && StringUtils.equals("-1", req.getStatus())) {
            req.setStatus(cnt > 0 ? "5" : "4");
        } else {
            req.setStatus(cnt > 0 ? "3" : "2");
        }
    }

    /**
     *
     */
    private void doUpdate(AssetsAssetOperateLogSaveReqVO req, PreContext ctx) {
        Map<String, Object> after = JSONUtils.convertTaskDefinitionJsonMap(req.getUpdateAfter());
        Map<String, Object> keys = JSONUtils.convertTaskDefinitionJsonMap(req.getFieldNames());
        List<String> setCols = JSONUtils.splitListByString(keys.get("tableCommentList"));
        List<String> whereCols = JSONUtils.splitListByString(keys.get("commentKeyList"));
        fillUpdateWhereMd5(req, after, whereCols);

        String setClause = setCols.stream()
                .map(colName -> formatExpression(colName, after.get(colName), findColumn(colName, ctx.columns), ctx.prop))
                .collect(Collectors.joining(","));
        String whereClause = whereCols.stream()
                .map(colName -> formatExpression(colName, after.get(colName), findColumn(colName, ctx.columns), ctx.prop))
                .collect(Collectors.joining(" AND "));

        String sql = sql_UPDATE
                .replace("{tableName}", ctx.fullTable)
                .replace("{setValue}", setClause)
                .replace("{where}", whereClause);
        log.info("UPDATE SQL: {}", sql);

        req.setExecuteTime(DateUtils.getExecutionDate());
        int cnt = ctx.query.update(sql);
        if (StringUtils.isNotEmpty(req.getStatus()) && StringUtils.equals("-1", req.getStatus())) {
            req.setStatus(cnt > 0 ? "5" : "4");
        } else {
            req.setStatus(cnt > 0 ? "3" : "2");
        }
    }

    /**
     *
     */
    private void doDelete(AssetsAssetOperateLogSaveReqVO req, PreContext ctx) {
        Map<String, Object> after = JSONUtils.convertTaskDefinitionJsonMap(req.getUpdateAfter());
        Map<String, Object> keys = JSONUtils.convertTaskDefinitionJsonMap(req.getFieldNames());
        // TODO: 按需补充
        List<String> whereCols = JSONUtils.splitListByString(keys.get("commentKeyList"));
        String whereClause = whereCols.stream()
                .map(colName -> formatExpression(colName, after.get(colName), findColumn(colName, ctx.columns), ctx.prop))
                .collect(Collectors.joining(" AND "));
        String sql = sql_DELETE
                .replace("{tableName}", ctx.fullTable)
                .replace("{where}", whereClause);
        log.info("UPDATE SQL: {}", sql);

        req.setExecuteTime(DateUtils.getExecutionDate());
        int cnt = ctx.query.update(sql);
        if (StringUtils.isNotEmpty(req.getStatus()) && StringUtils.equals("-1", req.getStatus())) {
            req.setStatus(cnt > 0 ? "5" : "4");
        } else {
            req.setStatus(cnt > 0 ? "3" : "2");
        }
    }

    /**
     *
     */
    private void doImport(AssetsAssetOperateLogSaveReqVO req, PreContext ctx) {
        // TODO: 按需补充
    }

    /**
     *  SET/WHERE
     */
    private static String formatExpression(String colName, Object val, DbColumn col, DbQueryProperty prop) {
        String expr;
        String timeType = DbDataType.checkTime(col.getDataType());
        if (val == null) {
            expr = "NULL";
        } else if (StringUtils.isNotBlank(timeType)) {
            DbDataType dt = DbDataType.getByDbTypeAndFieldType(prop.getDbType(), timeType);
            expr = dt.getSql().replace("${Data}", val.toString());
        } else if (isBooleanType(col.getDataType())) {
            expr = ((Boolean) val) ? "1" : "0";
        } else if (val instanceof Number) {
            expr = val.toString();
        } else {
            expr = "'" + val + "'";
        }
        return colName + " = " + expr;
    }

    private static String packFormatValue(Object v, String dataType, DbQueryProperty prop) {
        if (v == null) return "NULL";
        if (isBooleanType(dataType)) return ((Boolean) v) ? "1" : "0";
        return (v instanceof Number) ? v.toString() : "'" + v + "'";
    }

    private static boolean isBooleanType(String dt) {
        return "BOOLEAN".equalsIgnoreCase(dt) || "boolean".equalsIgnoreCase(dt);
    }

    private static DbColumn findColumn(String name, List<DbColumn> cols) {
        return cols.stream()
                .filter(c -> name.equals(c.getColName()))
                .findFirst()
                .orElseThrow(() -> new AssetOperateException(" " + name + " "));
    }

    /**
     *
     */
    private static class PreContext {
        final DbQuery query;
        final DbQueryProperty prop;
        final List<DbColumn> columns;
        final String fullTable;

        PreContext(DbQuery q, DbQueryProperty p, List<DbColumn> c, String ft) {
            this.query = q;
            this.prop = p;
            this.columns = c;
            this.fullTable = ft;
        }
    }

    /**
     *
     */
    public static class AssetOperateException extends RuntimeException {
        public AssetOperateException(String msg) {
            super(msg);
        }
    }
}
