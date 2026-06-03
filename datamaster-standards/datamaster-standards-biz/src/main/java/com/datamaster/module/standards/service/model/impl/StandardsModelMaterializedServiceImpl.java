package com.datamaster.module.standards.service.model.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.database.DataSourceFactory;
import com.datamaster.common.database.DbQuery;
import com.datamaster.common.database.constants.DbQueryProperty;
import com.datamaster.common.database.core.DbColumn;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.module.assets.api.asset.dto.AssetsAssetReqDTO;
import com.datamaster.module.assets.api.asset.dto.AssetsAssetRespDTO;
import com.datamaster.module.assets.api.service.asset.IAssetsAssetApiOutService;
import com.datamaster.module.standards.controller.admin.model.vo.*;
import com.datamaster.module.standards.dal.dataobject.model.StandardsModelColumnDO;
import com.datamaster.module.standards.dal.dataobject.model.StandardsModelDO;
import com.datamaster.module.standards.dal.dataobject.model.StandardsModelMaterializedDO;
import com.datamaster.module.standards.dal.mapper.model.StandardsModelMaterializedMapper;
import com.datamaster.module.standards.service.model.IStandardsModelColumnService;
import com.datamaster.module.standards.service.model.IStandardsModelMaterializedService;
import com.datamaster.module.standards.service.model.IStandardsModelService;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 物化模型记录Service业务层处理
 *
 * @author DATAMASTER
 * @date 2025-01-21
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class StandardsModelMaterializedServiceImpl  extends ServiceImpl<StandardsModelMaterializedMapper,StandardsModelMaterializedDO> implements IStandardsModelMaterializedService {
    @Resource
    private StandardsModelMaterializedMapper StandardsModelMaterializedMapper;
    @Resource
    private IStandardsModelColumnService StandardsModelColumnService;
    @Resource
    private IStandardsModelService StandardsModelService;
    @Resource
    private IAssetsAssetApiOutService iAssetsAssetApiService;

    @Autowired
    private DataSourceFactory dataSourceFactory;
    /**
     * 物化建表
     * @param StandardsModelMaterialized
     * @return
     */
    @Override
    public Long createMaterializedTable(StandardsMaterializedMethodReqVO StandardsModelMaterialized) {
        //取出模型id
        List<Long> modelIdList = StandardsModelMaterialized.getModelId();
        if(CollectionUtils.isEmpty(modelIdList)){
            throw new RuntimeException("获取信息失败,原因:物化信息为空");
        }

        DbQueryProperty dbQueryProperty = new DbQueryProperty(StandardsModelMaterialized.getDatasourceType()
                ,StandardsModelMaterialized.getIP(),StandardsModelMaterialized.getPort(),StandardsModelMaterialized.getDatasourceConfig());
        DbQuery dbQuery = dataSourceFactory.createDbQuery(dbQueryProperty);
        //测试链接
        if (!dbQuery.valid()) {
            throw new RuntimeException("数据库连接失败！");
        }

        for (Long modelId : modelIdList) {

            StandardsModelMaterializedDO StandardsModelMaterializedDO = this.anotherAsyncTaskSingle(modelId, StandardsModelMaterialized,dbQuery,dbQueryProperty);

            StandardsModelMaterializedDO.setCreatorId(StandardsModelMaterialized.getCreatorId());
            StandardsModelMaterializedDO.setCreateBy(StandardsModelMaterialized.getCreateBy());
            StandardsModelMaterializedDO.setCreateTime(StandardsModelMaterializedDO.getCreateTime());

            String status = StandardsModelMaterializedDO.getStatus();
            if(StringUtils.equals("3",status)){
                //资产
                AssetsAssetReqDTO assetReqDTO = new AssetsAssetReqDTO();
                assetReqDTO.setSource("2");
                assetReqDTO.setModelId(modelId);
                assetReqDTO.setDatasourceId(StandardsModelMaterialized.getDatasourceId());
                assetReqDTO.setFieldCount(StandardsModelMaterializedDO.getFieldCount());
                AssetsAssetRespDTO assetRespDTO = iAssetsAssetApiService.insertAsset(assetReqDTO);
                Long id = assetRespDTO.getId();//资产id
                StandardsModelMaterializedDO.setAssetId(id);
            }
            StandardsModelMaterializedMapper.insert(StandardsModelMaterializedDO);
        }
        dbQuery.close();

        return 1L;
    }

    /**
     * @param modelId
     * @param StandardsModelMaterialized
     * @param dbQuery
     * @param dbQueryProperty
     */
    private StandardsModelMaterializedDO anotherAsyncTaskSingle(Long modelId, StandardsMaterializedMethodReqVO StandardsModelMaterialized, DbQuery dbQuery, DbQueryProperty dbQueryProperty) {

        // 先创建一个日志对象，并设置一些基础信息；发生异常也能返回它
        StandardsModelMaterializedDO StandardsModelMaterializedDO = buildLogRecord(modelId, StandardsModelMaterialized);

        // 默认先给它一个状态：2=创建中
        StandardsModelMaterializedDO.setStatus("2");
        // 在往数据库插入前，建议先 setCreateTime / setCreateBy / setUpdateTime 等，根据需要

        try {
            // 1. 查询模型/字段，若校验不通过则抛异常
            StandardsModelDO StandardsModelDO = checkAndGetModel(modelId);
            StandardsModelMaterializedDO.setModelName(StandardsModelDO.getModelName());
            StandardsModelMaterializedDO.setModelAlias(StandardsModelDO.getModelComment());


            List<StandardsModelColumnDO> columnList = checkAndGetModelColumns(modelId);
            //设置字段数据量
            StandardsModelMaterializedDO.setFieldCount(Long.valueOf(columnList.size()));


            String tableName = StandardsModelDO.getModelName();
            int tableStatus = dbQuery.generateCheckTableExistsSQL(dbQueryProperty,tableName);
            if (tableStatus > 0) {
                //应产品要求改动为成功失败
                StandardsModelMaterializedDO.setStatus("4"); // 5=已存在
                StandardsModelMaterializedDO.setMessage("表 [" + tableName + "] 已存在，无需重复创建");
                return StandardsModelMaterializedDO;
            }
            List<DbColumn> dbColumns = this.setColumnsListFromDpModelColumns(columnList);

            List<String> tableSQLList = dbQuery.generateCreateTableSQL(dbQueryProperty
                    ,tableName, StandardsModelDO.getModelComment(),dbColumns);

            StandardsModelMaterializedDO.setSqlCommand(tableSQLList.toString()); // 记录一下执行SQL

            for (String sql : tableSQLList) {
                dbQuery.execute(sql);
            }

            // 若执行成功 -> 状态=3
            StandardsModelMaterializedDO.setStatus("3");
            StandardsModelMaterializedDO.setMessage("建表成功");

        } catch (Exception ex) {
            // 不管任何异常都要记录到日志中
            StandardsModelMaterializedDO.setStatus("4"); // 4=失败
            StandardsModelMaterializedDO.setMessage("建表失败：" + ex.getMessage());
        }

        return StandardsModelMaterializedDO;
    }


    /**
     * 将 StandardsModelColumnDO 转换为 DbColumn，并赋值给 columnsList
     * @param columnList StandardsModelColumnDO 列表
     */
    public List<DbColumn> setColumnsListFromDpModelColumns(List<StandardsModelColumnDO> columnList) {
        return  columnList.stream()
                .map(StandardsColumn -> DbColumn.builder()
                        .colName(StandardsColumn.getEngName())
                        .dataType(StandardsColumn.getColumnType())
                        .dataLength(StandardsColumn.getColumnLength() != null ? StandardsColumn.getColumnLength().toString() : null)
                        .dataScale(StandardsColumn.getColumnScale() != null ? StandardsColumn.getColumnScale().toString() : null)
                        .colKey("1".equals(StandardsColumn.getPkFlag()))
                        .nullable("0".equals(StandardsColumn.getNullableFlag()))
                        .colPosition(StandardsColumn.getSortOrder() == null? 1:StandardsColumn.getSortOrder().intValue())
                        .dataDefault(StandardsColumn.getDefaultValue())
                        .colComment(StandardsColumn.getCnName())  // 或者使用其它字段填充
                        .build())
                .collect(Collectors.toList());
    }


    /**
     * 组装一个日志记录对象，填充基础字段
     * @param modelId
     * @param StandardsModelMaterialized
     * @return
     */
    private StandardsModelMaterializedDO buildLogRecord(Long modelId, StandardsMaterializedMethodReqVO StandardsModelMaterialized) {
        StandardsModelMaterializedDO logRecord = new StandardsModelMaterializedDO();
        logRecord.setModelId(modelId);
        logRecord.setDatasourceId(StandardsModelMaterialized.getDatasourceId().toString());
        logRecord.setDatasourceType(StandardsModelMaterialized.getDatasourceType());
        logRecord.setDatasourceName(StandardsModelMaterialized.getDatasourceName());
        logRecord.setValidFlag(true);
        logRecord.setDelFlag(false);
        return logRecord;
    }

    /**
     * 查询并校验 StandardsModelDO，不存在则抛异常
     */
    private StandardsModelDO checkAndGetModel(Long modelId) {
        StandardsModelDO StandardsModelDO = StandardsModelService.getById(modelId);
        if (StandardsModelDO == null) {
            throw new RuntimeException("逻辑模型不存在, modelId=" + modelId);
        }
        return StandardsModelDO;
    }


    /**
     * 查询并校验字段列表，不存在则抛异常
     */
    private List<StandardsModelColumnDO> checkAndGetModelColumns(Long modelId) {
        StandardsModelColumnSaveReqVO reqVO = new StandardsModelColumnSaveReqVO();
        reqVO.setModelId(modelId);
        List<StandardsModelColumnDO> columnList = StandardsModelColumnService.getDpModelColumnList(reqVO);
        if (CollectionUtils.isEmpty(columnList)) {
            throw new RuntimeException("逻辑模型无字段，无法建表, modelId=" + modelId);
        }
        return columnList;
    }


    @Override
    public PageResult<StandardsModelMaterializedDO> getDpModelMaterializedPage(StandardsModelMaterializedPageReqVO pageReqVO) {
        return StandardsModelMaterializedMapper.selectPage(pageReqVO);
    }

    @Override
    public Long createDpModelMaterialized(StandardsModelMaterializedSaveReqVO createReqVO) {
        StandardsModelMaterializedDO dictType = BeanUtils.toBean(createReqVO, StandardsModelMaterializedDO.class);
        StandardsModelMaterializedMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public int updateDpModelMaterialized(StandardsModelMaterializedSaveReqVO updateReqVO) {
        // 相关校验

        // 更新物化模型记录
        StandardsModelMaterializedDO updateObj = BeanUtils.toBean(updateReqVO, StandardsModelMaterializedDO.class);
        return StandardsModelMaterializedMapper.updateById(updateObj);
    }
    @Override
    public int removeDpModelMaterialized(Collection<Long> idList) {
        // 批量删除物化模型记录
        return StandardsModelMaterializedMapper.deleteBatchIds(idList);
    }

    @Override
    public StandardsModelMaterializedDO getDpModelMaterializedById(Long id) {
        return StandardsModelMaterializedMapper.selectById(id);
    }

    @Override
    public List<StandardsModelMaterializedDO> getDpModelMaterializedList() {
        return StandardsModelMaterializedMapper.selectList();
    }

    @Override
    public Map<Long, StandardsModelMaterializedDO> getDpModelMaterializedMap() {
        List<StandardsModelMaterializedDO> StandardsModelMaterializedList = StandardsModelMaterializedMapper.selectList();
        return StandardsModelMaterializedList.stream()
                .collect(Collectors.toMap(
                        StandardsModelMaterializedDO::getId,
                        StandardsModelMaterializedDO -> StandardsModelMaterializedDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing
                ));
    }


        /**
         * 导入物化模型记录数据
         *
         * @param importExcelList 物化模型记录数据列表
         * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
         * @param operName 操作用户
         * @return 结果
         */
        @Override
        public String importDpModelMaterialized(List<StandardsModelMaterializedRespVO> importExcelList, boolean isUpdateSupport, String operName) {
            if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
                throw new ServiceException("导入数据不能为空！");
            }

            int successNum = 0;
            int failureNum = 0;
            List<String> successMessages = new ArrayList<>();
            List<String> failureMessages = new ArrayList<>();

            for (StandardsModelMaterializedRespVO respVO : importExcelList) {
                try {
                    StandardsModelMaterializedDO StandardsModelMaterializedDO = BeanUtils.toBean(respVO, StandardsModelMaterializedDO.class);
                    Long StandardsModelMaterializedId = respVO.getId();
                    if (isUpdateSupport) {
                        if (StandardsModelMaterializedId != null) {
                            StandardsModelMaterializedDO existingDpModelMaterialized = StandardsModelMaterializedMapper.selectById(StandardsModelMaterializedId);
                            if (existingDpModelMaterialized != null) {
                                StandardsModelMaterializedMapper.updateById(StandardsModelMaterializedDO);
                                successNum++;
                                successMessages.add("数据更新成功，ID为 " + StandardsModelMaterializedId + " 的物化模型记录记录。");
                            } else {
                                failureNum++;
                                failureMessages.add("数据更新失败，ID为 " + StandardsModelMaterializedId + " 的物化模型记录记录不存在。");
                            }
                        } else {
                            failureNum++;
                            failureMessages.add("数据更新失败，某条记录的ID不存在。");
                        }
                    } else {
                        QueryWrapper<StandardsModelMaterializedDO> queryWrapper = new QueryWrapper<>();
                        queryWrapper.eq("id", StandardsModelMaterializedId);
                        StandardsModelMaterializedDO existingDpModelMaterialized = StandardsModelMaterializedMapper.selectOne(queryWrapper);
                        if (existingDpModelMaterialized == null) {
                            StandardsModelMaterializedMapper.insert(StandardsModelMaterializedDO);
                            successNum++;
                            successMessages.add("数据插入成功，ID为 " + StandardsModelMaterializedId + " 的物化模型记录记录。");
                        } else {
                            failureNum++;
                            failureMessages.add("数据插入失败，ID为 " + StandardsModelMaterializedId + " 的物化模型记录记录已存在。");
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

}
