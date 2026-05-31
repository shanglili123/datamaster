package com.datamaster.module.catalog.service.metadata.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import com.datamaster.common.core.domain.BatchDeleteCheck;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.module.assets.api.service.asset.IAssetsAssetApiOutService;
import com.datamaster.module.catalog.controller.admin.metadata.vo.CatalogDbRespVO;
import com.datamaster.module.catalog.controller.admin.metadata.vo.CatalogTablePageReqVO;
import com.datamaster.module.catalog.controller.admin.metadata.vo.CatalogTableRespVO;
import com.datamaster.module.catalog.controller.admin.metadata.vo.CatalogTableSaveReqVO;
import com.datamaster.module.catalog.dal.dataobject.metadata.CatalogColumnDO;
import com.datamaster.module.catalog.dal.dataobject.metadata.CatalogDbDO;
import com.datamaster.module.catalog.dal.dataobject.metadata.CatalogTableDO;
import com.datamaster.module.catalog.dal.dataobject.task.CatalogTaskDO;
import com.datamaster.module.catalog.dal.mapper.metadata.CatalogColumnMapper;
import com.datamaster.module.catalog.dal.mapper.metadata.CatalogDbMapper;
import com.datamaster.module.catalog.dal.mapper.metadata.CatalogTableMapper;
import com.datamaster.module.catalog.service.metadata.ICatalogColumnService;
import com.datamaster.module.catalog.service.metadata.ICatalogDbService;
import com.datamaster.module.catalog.service.metadata.ICatalogTableService;
import com.datamaster.module.catalog.service.task.ICatalogTaskService;
import com.datamaster.module.system.service.ISysUserService;

import javax.annotation.Resource;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 元数据信息Service业务层处理
 *
 * @author DATAMASTER
 * @date 2026-02-11
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class CatalogTableServiceImpl extends ServiceImpl<CatalogTableMapper,CatalogTableDO> implements ICatalogTableService {
    @Resource
    private CatalogTableMapper CatalogTableMapper;
    @Resource
    private CatalogColumnMapper columnMapper;
    @Resource
    private ICatalogDbService dbService;
    @Resource
    @Lazy
    private ICatalogColumnService CatalogColumnService;
    @Resource
    private CatalogDbMapper CatalogDbMapper;
    @Resource
    private ISysUserService sysUserService;
    @Resource
    private IAssetsAssetApiOutService daAssetApiOutService;
    @Resource
    private ICatalogTaskService CatalogTaskService;

    @Override
    public PageResult<CatalogTableDO> getCatalogTablePage(CatalogTablePageReqVO pageReqVO) {
        PageResult<CatalogTableDO> CatalogTablelist = CatalogTableMapper.selectPage(pageReqVO);
        return CatalogTablelist;
    }

    @Override
    public PageResult<CatalogTableRespVO> getCatalogTablePageAsset(CatalogTablePageReqVO CatalogTable) {
        PageResult<CatalogTableDO> CatalogTablelist = CatalogTableMapper.getCatalogTablelist(CatalogTable);
        PageResult<CatalogTableRespVO> bean = BeanUtils.toBean(CatalogTablelist, CatalogTableRespVO.class);
        List<CatalogTableRespVO> rows = bean.getRows();
        for (CatalogTableRespVO row : rows) {
            MPJLambdaWrapper<CatalogColumnDO> wrapper = new MPJLambdaWrapper<>();
            wrapper.eq(CatalogColumnDO::getTableId, row.getId());
            long count = CatalogColumnService.count(wrapper);
            row.setColumnCount(count);
        }
        bean.setRows(rows);
        return bean;
    }

    @Override
    public List<CatalogTableRespVO> getCatalogTableListAsset(CatalogTablePageReqVO CatalogTable) {
        List<CatalogTableDO> CatalogTablelist = CatalogTableMapper.getCatalogTableListAsset(CatalogTable);
        List<CatalogTableRespVO> rows = BeanUtils.toBean(CatalogTablelist, CatalogTableRespVO.class);
        return rows;
    }

    @Override
    public Long createCatalogTable(CatalogTableSaveReqVO createReqVO) {
        CatalogTableDO CatalogTableDO = BeanUtils.toBean(createReqVO, CatalogTableDO.class);
        // 获取数据库元数据信息，包括数据库类型
        CatalogTableDO = reCatalogTableDO(CatalogTableDO);
        CatalogTableMapper.insert(CatalogTableDO);
        return CatalogTableDO.getId();
    }


    @Override
    public int updateCatalogTable(CatalogTableSaveReqVO updateReqVO) {
        // 相关校验

        // 更新元数据信息
        CatalogTableDO updateCatalogTableDO = BeanUtils.toBean(updateReqVO, CatalogTableDO.class);
        // 获取数据库元数据信息，包括数据库类型
        updateCatalogTableDO = reCatalogTableDO(updateCatalogTableDO);
        return CatalogTableMapper.updateById(updateCatalogTableDO);
    }
    private CatalogTableDO reCatalogTableDO(CatalogTableDO CatalogTableDO) {
        CatalogDbDO CatalogDbDO = CatalogDbMapper.findById(CatalogTableDO.getDbId());
//        if (CatalogDbDO != null) {
//            // 使用数据库方言获取表的行数、索引、分区字段等信息
//            DatabaseDialect dialect = DatabaseDialectFactory.getDialect(CatalogDbDO);
//            if (dialect != null) {
//                // 批量获取表元数据信息
//                DatabaseDialect.TableMetadata metadata = dialect.getTableMetadata(CatalogDbDO, CatalogTableDO.getTableName());
//                CatalogTableDO.setRowCount(metadata.getRowCount());
//                CatalogTableDO.setTbIndex(metadata.getIndexes());
//                CatalogTableDO.setPartitionKey(metadata.getPartitionFields());
//                CatalogTableDO.setStorageSize(null !=metadata.getTableSize()?metadata.getTableSize().intValue(): null);
//                CatalogTableDO.setStorageEngine(metadata.getStorageEngine());
//                CatalogTableDO.setTableComment(metadata.getTableComment());
//                CatalogTableDO.setPrimaryKey(metadata.getPrimaryKey());
//                CatalogTableDO.setTbCreateTime(metadata.getCreateTime() != null ? parseDate(metadata.getCreateTime()) : null);
//                CatalogTableDO.setDataUpdateTime(metadata.getUpdateTime() != null ? parseDate(metadata.getUpdateTime()) : null);
//
//            }
//        }
        return CatalogTableDO;
    }
    private Date parseDate(String dateStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return sdf.parse(dateStr);
        } catch (ParseException e) {
            // 处理日期解析异常，例如返回 null 或记录日志
            log.error("日期解析异常：{}", e.getMessage());
            return null;
        }
    }
    @Override
    public int removeCatalogTable(Collection<Long> idList) {
        // 批量删除元数据信息
        //return CatalogTableMapper.deleteBatchIds(idList);
        // 批量删除表元数据信息
        //if (columnMapper.existsByTableIds(idList)) {
        //    throw new ServiceException("被字段元数据引用，不可删除");
        //}
        return CatalogTableMapper.delete(Wrappers.lambdaQuery(CatalogTableDO.class)
                .in(CatalogTableDO::getId, idList)
                .eq(CatalogTableDO::getStatus, "0"));
    }

    @Override
    public CatalogTableRespVO getCatalogTableById(Long id) {
        //return CatalogTableMapper.selectById(id);
        CatalogTableDO tableDO = CatalogTableMapper.findById(id);
        if (tableDO == null) {
            return null;
        }
        CatalogTableRespVO respVO = BeanUtils.toBean(tableDO, CatalogTableRespVO.class);
        if (tableDO.getDbId() != null) {
            CatalogDbRespVO mdDbRespVO = dbService.getCatalogDbById(tableDO.getDbId());
            respVO.setDbRespVO(mdDbRespVO);
            respVO.setSourceSystemName(mdDbRespVO.getSourceSystemName());
            respVO.setSourceSystemId(mdDbRespVO.getSourceSystemId());

/*            // 获取数据库元数据信息，包括数据库类型
            CatalogDbDO CatalogDbDO = CatalogDbMapper.findById(tableDO.getDbId());
            if (CatalogDbDO != null) {
                CatalogTableDO CatalogTableDO = reCatalogTableDO(tableDO);
                if (tableDO != null) {
                    // 批量获取表元数据信息
                    respVO.setRowCount(CatalogTableDO.getRowCount());
                    respVO.setTbIndex(CatalogTableDO.getTbIndex());
                    respVO.setPartitionKey(CatalogTableDO.getPartitionKey());
                    respVO.setStorageSize(CatalogTableDO.getStorageSize());
                    respVO.setStorageEngine(CatalogTableDO.getStorageEngine());
                }
            }*/
        }
        if (tableDO.getTaskId() != null) {
            CatalogTaskDO CatalogTaskById = CatalogTaskService.getCatalogTaskById(tableDO.getTaskId());
            if (CatalogTaskById != null) {
                respVO.setSourceSystemId(CatalogTaskById.getSourceSystemId());
                respVO.setSourceSystemName(CatalogTaskById.getSourceSystemName());
            }
        }
        MPJLambdaWrapper<CatalogColumnDO> wrapper = new MPJLambdaWrapper<>();
        wrapper.eq(CatalogColumnDO::getTableId, tableDO.getId());
        long count = CatalogColumnService.count(wrapper);
        respVO.setColumnCount(count);
        return respVO;
    }

    @Override
    public List<CatalogTableRespVO> getCatalogTableById(CatalogTableRespVO createReqVO) {
        MPJLambdaWrapper<CatalogTableDO> wrapper = new MPJLambdaWrapper<>();
        wrapper.eq(CatalogTableDO::getTaskId, createReqVO.getTaskId()).eq(CatalogTableDO::getDbId, createReqVO.getDbId()).eq(StringUtils.isNotEmpty(createReqVO.getSchemaName()), CatalogTableDO::getSchemaName, createReqVO.getSchemaName()).eq(StringUtils.isNotEmpty(createReqVO.getDbName()), CatalogTableDO::getDbName, createReqVO.getDbName()).eq(CatalogTableDO::getDelFlag, "0");

        List<CatalogTableDO> mdDbDOS = CatalogTableMapper.selectList(wrapper);

        return CollectionUtils.isEmpty(mdDbDOS) ? new ArrayList<>() : BeanUtils.toBean(mdDbDOS, CatalogTableRespVO.class);
    }
    @Override
    public List<CatalogTableDO> getCatalogTableList() {
        return CatalogTableMapper.selectList();
    }

    @Override
    public Map<Long, CatalogTableDO> getCatalogTableMap() {
        List<CatalogTableDO> CatalogTableList = CatalogTableMapper.selectList();
        return CatalogTableList.stream()
                .collect(Collectors.toMap(
                        CatalogTableDO::getId,
                        CatalogTableDO -> CatalogTableDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing
                ));
    }

    @Override
    public List<CatalogTableRespVO> getCatalogTableByDbId(Collection<Long> idList) {
        MPJLambdaWrapper<CatalogTableDO> wrapper = new MPJLambdaWrapper<>();
        wrapper.in(CatalogTableDO::getDbId, idList).eq(CatalogTableDO::getDelFlag, "0");

        List<CatalogTableDO> mdDbDOS = CatalogTableMapper.selectList(wrapper);

        return CollectionUtils.isEmpty(mdDbDOS) ? new ArrayList<>() : BeanUtils.toBean(mdDbDOS, CatalogTableRespVO.class);
    }

    @Override
    public Long saveDraft(CatalogTableSaveReqVO saveReqVO) {
        CatalogTableDO tableDO = BeanUtils.toBean(saveReqVO, CatalogTableDO.class);
        if (tableDO.getId() == null) {
            CatalogTableMapper.insert(tableDO);
        } else {
            CatalogTableMapper.updateById(tableDO);
        }
        return tableDO.getId();
    }

    @Override
    public Integer toggle(Long id, String status) {
        CatalogTableDO update = new CatalogTableDO();
        update.setId(id);
        update.setStatus(status);
        return CatalogTableMapper.updateById(update);
    }

    @Override
    public BatchDeleteCheck<Long> batchDeleteCheck(List<Long> ids) {
        List<CatalogTableDO> list = baseMapper.selectBatchIds(ids);
        int cannotDeleteCount = 0;
        List<Long> canDeleteIds = new ArrayList<>();
        for (CatalogTableDO one : list) {
            if ("1".equals(one.getStatus())) {
                cannotDeleteCount++;
                continue;
            }
            // 检查是否有字段引用 检查是否被数据资产使用（通过tableId）
            //boolean hasColumn = columnMapper.existsByTableId(one.getId());
            //boolean usedByAsset = daAssetApiOutService.existsByTableId(one.getId());
            //if (false) {
            //    cannotDeleteCount++;
            //} else {
                canDeleteIds.add(one.getId());
            //}
        }
        return new BatchDeleteCheck<>(cannotDeleteCount, canDeleteIds);
    }

}
