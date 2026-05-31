package com.datamaster.module.catalog.service.metadata.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import com.datamaster.common.core.domain.BaseEntity;
import com.datamaster.common.core.domain.BatchDeleteCheck;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.module.catalog.api.column.dto.CatalogColumnRespDTO;
import com.datamaster.module.catalog.api.service.column.CatalogColumnApiService;
import com.datamaster.module.catalog.controller.admin.metadata.vo.*;
import com.datamaster.module.catalog.dal.dataobject.metadata.CatalogColumnDO;
import com.datamaster.module.catalog.dal.dataobject.metadata.CatalogDbDO;
import com.datamaster.module.catalog.dal.mapper.metadata.CatalogColumnMapper;
import com.datamaster.module.catalog.dal.mapper.metadata.CatalogDbMapper;
import com.datamaster.module.catalog.service.metadata.ICatalogColumnService;
import com.datamaster.module.catalog.service.metadata.ICatalogDbService;
import com.datamaster.module.catalog.service.metadata.ICatalogTableService;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 元数据字段信息Service业务层处理
 *
 * @author DATAMASTER
 * @date 2026-02-11
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class CatalogColumnServiceImpl extends ServiceImpl<CatalogColumnMapper, CatalogColumnDO> implements ICatalogColumnService, CatalogColumnApiService {
    @Resource
    private CatalogColumnMapper CatalogColumnMapper;
    @Resource
    private CatalogDbMapper CatalogDbMapper;
    @Resource
    private ICatalogTableService tableService;
    @Resource
    private ICatalogDbService dbService;

    @Override
    public PageResult<CatalogColumnDO> getCatalogColumnPage(CatalogColumnPageReqVO pageReqVO) {
        return CatalogColumnMapper.selectPage(pageReqVO);
    }

    @Override
    public Long createCatalogColumn(CatalogColumnSaveReqVO createReqVO) {
        CatalogColumnDO CatalogColumnDO = BeanUtils.toBean(createReqVO, CatalogColumnDO.class);
        CatalogColumnMapper.insert(CatalogColumnDO);
        return CatalogColumnDO.getId();
    }

    @Override
    public List<CatalogColumnDO> createCatalogColumnList(List<CatalogColumnSaveReqVO> createReqVO) {
        List<CatalogColumnDO> columnDO = BeanUtils.toBean(createReqVO, CatalogColumnDO.class);
        if (null != columnDO && columnDO.size() > 0) {
            for (CatalogColumnDO CatalogColumnDO : columnDO) {
                // 获取数据库元数据信息，包括数据库类型
                CatalogColumnDO = reCatalogColumnDO(CatalogColumnDO);
            }
        }
        CatalogColumnMapper.insertBatch(columnDO);
        return columnDO;
    }

    private CatalogColumnDO reCatalogColumnDO(CatalogColumnDO CatalogColumnDO) {
        CatalogDbDO CatalogDbDO = CatalogDbMapper.findById(CatalogColumnDO.getDbId());

        CatalogColumnRespVO respVO = BeanUtils.toBean(CatalogColumnDO, CatalogColumnRespVO.class);
        if (CatalogColumnDO.getTableId() != null) {
            CatalogTableRespVO tableRespVO = tableService.getCatalogTableById(CatalogColumnDO.getTableId());
            respVO.setTableRespVO(tableRespVO);
        }

//        if (CatalogDbDO != null) {
//            // 使用数据库方言获取字段的自增和分区字段信息
//            DatabaseDialect dialect = DatabaseDialectFactory.getDialect(CatalogDbDO);
//            if (dialect != null) {
//                // 批量获取字段元数据信息
//                DatabaseDialect.ColumnMetadata metadata = dialect.getColumnMetadata(CatalogDbDO, respVO.getTableRespVO()
//                        .getTableName(), CatalogColumnDO.getColumnName());
//                // 设置字段自增信息
//                CatalogColumnDO.setAutoIncrementFlag(metadata.isAutoIncrement() ? "1" : "0");
//
//                // 设置字段是否为分区字段
//                DatabaseDialect.TableMetadata metadataTb = dialect.getTableMetadata(CatalogDbDO, respVO.getTableRespVO()
//                        .getTableName());
//                // 获取表的分区字段判断是否包含
//                Boolean partitionFields = false;
//                if (metadataTb.getPartitionFields() != null) {
//                    partitionFields = metadataTb.getPartitionFields()
//                            .toUpperCase()
//                            .contains(CatalogColumnDO.getColumnName().toUpperCase());
//                }
//                CatalogColumnDO.setPartitionFlag(partitionFields ? "1" : "0");
//                CatalogColumnDO.setUniqueFlag(metadata.isUnique() ? "1" : "0");
//            }
//        }
        return CatalogColumnDO;
    }

    @Override
    public int updateCatalogColumn(CatalogColumnSaveReqVO updateReqVO) {
        // 相关校验

        // 更新元数据字段信息
        CatalogColumnDO updateCatalogColumnDO = BeanUtils.toBean(updateReqVO, CatalogColumnDO.class);
        return CatalogColumnMapper.updateById(updateCatalogColumnDO);
    }

    @Override
    public int removeCatalogColumn(Collection<Long> idList) {
        // 批量删除元数据字段信息
        // return CatalogColumnMapper.deleteBatchIds(idList);
        return CatalogColumnMapper.delete(Wrappers.lambdaQuery(CatalogColumnDO.class)
                .in(CatalogColumnDO::getId, idList)
                .eq(CatalogColumnDO::getStatus, "0"));
    }

    @Override
    public int removeCatalogColumn(CatalogColumnRespVO CatalogColumnRespVO) {

        MPJLambdaWrapper<CatalogColumnDO> wrapper = new MPJLambdaWrapper<>();
        wrapper.eq(CatalogColumnDO::getTaskId, CatalogColumnRespVO.getTaskId())
                .eq(CatalogColumnDO::getTableId, CatalogColumnRespVO.getTableId())
                .eq(CatalogColumnDO::getDelFlag, "0");
        CatalogColumnMapper.delete(wrapper);
        return 1;
    }

    @Override
    public CatalogColumnRespVO getCatalogColumnById(Long id) {
        // return CatalogColumnMapper.selectById(id);
        CatalogColumnDO columnDO = CatalogColumnMapper.findById(id);
        if (columnDO == null) {
            return null;
        }
        CatalogColumnRespVO respVO = BeanUtils.toBean(columnDO, CatalogColumnRespVO.class);
        if (respVO.getTableId() != null) {
            CatalogTableRespVO tableRespVO = tableService.getCatalogTableById(respVO.getTableId());
            respVO.setTableRespVO(tableRespVO);
        }
        if (respVO.getDbId() != null) {
            CatalogDbRespVO mdDbRespVO = dbService.getCatalogDbById(columnDO.getDbId());
            respVO.setSourceSystemName(mdDbRespVO.getSourceSystemName());
            respVO.setSourceSystemId(mdDbRespVO.getSourceSystemId());

/*            // 获取数据库元数据信息，包括数据库类型
            CatalogColumnDTO CatalogColumnDO = reCatalogColumnDO(columnDO);
            if (CatalogColumnDO != null) {
                    // 设置字段自增信息
                    respVO.setAutoIncrementFlag(CatalogColumnDO.getAutoIncrementFlag());
                    // 设置字段是否为分区字段
                    respVO.setPartitionFlag(CatalogColumnDO.getPartitionFlag());
            }*/
        }
        return respVO;
    }

    @Override
    public List<CatalogColumnDO> getCatalogColumnList() {
        return CatalogColumnMapper.selectList();
    }

    @Override
    public List<CatalogColumnRespVO> getCatalogColumnList(CatalogColumnRespVO CatalogColumnRespVO) {

        MPJLambdaWrapper<CatalogColumnDO> wrapper = new MPJLambdaWrapper<>();
        wrapper.eq(CatalogColumnDO::getTaskId, CatalogColumnRespVO.getTaskId())
                .eq(CatalogColumnDO::getTableId, CatalogColumnRespVO.getTableId())
                .eq(CatalogColumnDO::getDelFlag, "0");

        List<CatalogColumnDO> mdDbDOS = CatalogColumnMapper.selectList(wrapper);

        return CollectionUtils.isEmpty(mdDbDOS) ? new ArrayList<>() : BeanUtils.toBean(mdDbDOS, CatalogColumnRespVO.class);

    }

    @Override
    public Map<Long, CatalogColumnDO> getCatalogColumnMap() {
        List<CatalogColumnDO> CatalogColumnList = CatalogColumnMapper.selectList();
        return CatalogColumnList.stream()
                .collect(Collectors.toMap(
                        CatalogColumnDO::getId,
                        CatalogColumnDO -> CatalogColumnDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing
                ));
    }

    @Override
    public List<CatalogColumnDO> getMdColumnList(CatalogColumnPageReqVO mdColumn) {
        MPJLambdaWrapper<CatalogColumnDO> wrapper = new MPJLambdaWrapper<>();

        wrapper.selectAll(CatalogColumnDO.class)
                // ===== 固定条件 =====
                .eq(CatalogColumnDO::getDelFlag, "0")

                // ===== 等值条件（if）=====
                .eq(mdColumn.getId() != null, CatalogColumnDO::getId, mdColumn.getId())
                .eq(mdColumn.getTaskId() != null, CatalogColumnDO::getTaskId, mdColumn.getTaskId())
                .eq(mdColumn.getDbId() != null, CatalogColumnDO::getDbId, mdColumn.getDbId())
                .eq(mdColumn.getTableId() != null, CatalogColumnDO::getTableId, mdColumn.getTableId())
                .eq(mdColumn.getDatasourceId() != null, CatalogColumnDO::getDatasourceId, mdColumn.getDatasourceId())
                .eq(mdColumn.getVersion() != null, CatalogColumnDO::getVersion, mdColumn.getVersion())
                .eq(mdColumn.getDataElemId() != null, CatalogColumnDO::getDataElemId, mdColumn.getDataElemId())
                .eq(mdColumn.getSafetyLevelId() != null, CatalogColumnDO::getSafetyLevelId, mdColumn.getSafetyLevelId())
                .eq(mdColumn.getBusinessLeader() != null, CatalogColumnDO::getBusinessLeader, mdColumn.getBusinessLeader())
                .eq(mdColumn.getResponsibleDept() != null, CatalogColumnDO::getResponsibleDept, mdColumn.getResponsibleDept())
                .eq(StringUtils.isNotBlank(mdColumn.getColumnType()), CatalogColumnDO::getColumnType, mdColumn.getColumnType())
                .eq(StringUtils.isNotBlank(mdColumn.getPkFlag()), CatalogColumnDO::getPkFlag, mdColumn.getPkFlag())
                .eq(StringUtils.isNotBlank(mdColumn.getFkFlag()), CatalogColumnDO::getFkFlag, mdColumn.getFkFlag())
                .eq(StringUtils.isNotBlank(mdColumn.getNullableFlag()), CatalogColumnDO::getNullableFlag, mdColumn.getNullableFlag())
                .eq(StringUtils.isNotBlank(mdColumn.getAuditStatus()), CatalogColumnDO::getAuditStatus, mdColumn.getAuditStatus())
                .eq(StringUtils.isNotBlank(mdColumn.getStatus()), CatalogColumnDO::getStatus, mdColumn.getStatus())

                // ===== like 条件 =====
                .like(StringUtils.isNotBlank(mdColumn.getColumnName()), CatalogColumnDO::getColumnName, mdColumn.getColumnName())
                .like(StringUtils.isNotBlank(mdColumn.getColumnComment()), CatalogColumnDO::getColumnComment, mdColumn.getColumnComment())
                .like(StringUtils.isNotBlank(mdColumn.getBusinessDefinition()), CatalogColumnDO::getBusinessDefinition, mdColumn.getBusinessDefinition())
                .like(StringUtils.isNotBlank(mdColumn.getMeasuringUnit()), CatalogColumnDO::getMeasuringUnit, mdColumn.getMeasuringUnit())
                .like(StringUtils.isNotBlank(mdColumn.getDefaultValue()), CatalogColumnDO::getDefaultValue, mdColumn.getDefaultValue())
                .like(StringUtils.isNotBlank(mdColumn.getDescription()), CatalogColumnDO::getDescription, mdColumn.getDescription())

                // ===== 数值条件 =====
                .eq(mdColumn.getColumnLength() != null, CatalogColumnDO::getColumnLength, mdColumn.getColumnLength())
                .eq(mdColumn.getColumnPrecision() != null, CatalogColumnDO::getColumnPrecision, mdColumn.getColumnPrecision())
                .eq(mdColumn.getColumnScale() != null, CatalogColumnDO::getColumnScale, mdColumn.getColumnScale())
                .eq(mdColumn.getDataQuality() != null, CatalogColumnDO::getDataQuality, mdColumn.getDataQuality())

                // ===== 时间 =====
                .orderByStr(org.apache.commons.lang3.StringUtils.isNotBlank(mdColumn.getOrderByColumn()), org.apache.commons.lang3.StringUtils.equals("asc", mdColumn.getIsAsc()), org.apache.commons.lang3.StringUtils.isNotBlank(mdColumn.getOrderByColumn()) ? Arrays.asList(mdColumn.getOrderByColumn()
                                                                                                                                                                                                                                                                                .split(",")) : null);

        List<CatalogColumnDO> list = CatalogColumnMapper.selectList(wrapper);
        return CollectionUtils.isEmpty(list) ? new ArrayList<>() : list;
    }

    @Override
    public Integer createMdColumn(List<CatalogColumnSaveReqVO> createReqVO) {
        Long tableId = createReqVO.get(0).getTableId();
        List<CatalogColumnDO> columnDOs = BeanUtils.toBean(createReqVO, CatalogColumnDO.class);
        Set<String> columnNames = columnDOs.stream().map(CatalogColumnDO::getColumnName).collect(Collectors.toSet());
        if (columnNames.size() != columnDOs.size()) {
            throw new ServiceException("字段名重复");
        }
        List<CatalogColumnDO> exists = CatalogColumnMapper.findByTableIdAndColumnNameIn(tableId, columnNames);
        if (!exists.isEmpty()) {
            String ex = exists.stream().map(CatalogColumnDO::getColumnName).collect(Collectors.joining(","));
            throw new ServiceException("与同表的其他字段名重复, [" + ex + "]");
        }
        CatalogColumnMapper.insertBatch(columnDOs);
        return createReqVO.size();
    }

    @Override
    public Integer saveDraft(List<CatalogColumnSaveReqVO> saveReqVO) {
        List<CatalogColumnDO> columnDO = BeanUtils.toBean(saveReqVO, CatalogColumnDO.class);
        CatalogColumnMapper.insertBatch(columnDO);
        return columnDO.size();
    }

    @Override
    public Integer toggle(Long id, String status) {
        CatalogColumnDO update = new CatalogColumnDO();
        update.setId(id);
        update.setStatus(status);
        return CatalogColumnMapper.updateById(update);
    }

    @Override
    public BatchDeleteCheck<Long> batchDeleteCheck(List<Long> ids) {
        List<CatalogColumnDO> list = baseMapper.selectBatchIds(ids);
        int cannotDeleteCount = 0;
        List<Long> canDeleteIds = new ArrayList<>();
        for (CatalogColumnDO one : list) {
            if ("1".equals(one.getStatus())) {
                cannotDeleteCount++;
                continue;
            }
            canDeleteIds.add(one.getId());
        }
        return new BatchDeleteCheck<>(cannotDeleteCount, canDeleteIds);
    }

    @Override
    public boolean existsByDataElemIds(Collection<Long> dataElemIds) {
        return baseMapper.existsByDataElemIds(dataElemIds);
    }

    @Override
    public List<CatalogColumnRespDTO> listByTableId(Long tableId) {
        List<CatalogColumnDO> CatalogColumnDOList = baseMapper.selectList(Wrappers.lambdaQuery(CatalogColumnDO.class)
                .eq(CatalogColumnDO::getTableId, tableId)
                .orderByAsc(BaseEntity::getCreateTime));
        return BeanUtils.toBean(CatalogColumnDOList, CatalogColumnRespDTO.class);
    }
}
