package com.datamaster.module.catalog.service.metadata.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import com.datamaster.common.core.domain.BatchDeleteCheck;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.module.assets.api.datasource.dto.AssetsDatasourceRespDTO;
import com.datamaster.module.assets.api.service.asset.IAssetsDatasourceApiService;
import com.datamaster.module.catalog.controller.admin.metadata.vo.CatalogDbPageReqVO;
import com.datamaster.module.catalog.controller.admin.metadata.vo.CatalogDbRespVO;
import com.datamaster.module.catalog.controller.admin.metadata.vo.CatalogDbSaveReqVO;
import com.datamaster.module.catalog.controller.admin.metadata.vo.CatalogMetaSearchRespDTO;
import com.datamaster.module.catalog.dal.dataobject.metadata.CatalogDbDO;
import com.datamaster.module.catalog.dal.mapper.metadata.CatalogDbMapper;
import com.datamaster.module.catalog.dal.mapper.metadata.CatalogTableMapper;
import com.datamaster.module.catalog.service.metadata.ICatalogColumnService;
import com.datamaster.module.catalog.service.metadata.ICatalogDbService;
import com.datamaster.module.catalog.service.metadata.ICatalogTableService;
import com.datamaster.mybatis.core.util.ForceUpdateHelper;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据库Service业务层处理
 *
 * @author DATAMASTER
 * @date 2026-02-11
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class CatalogDbServiceImpl  extends ServiceImpl<CatalogDbMapper,CatalogDbDO> implements ICatalogDbService {
    @Resource
    private CatalogDbMapper CatalogDbMapper;
    @Resource
    private IAssetsDatasourceApiService daDatasourceApiService;

    @Resource
    private CatalogTableMapper tableMapper;
    @Resource
    private ICatalogTableService CatalogTableService;

    @Resource
    private ICatalogColumnService CatalogColumnService;




    @Override
    public PageResult<CatalogDbDO> getCatalogDbPage(CatalogDbPageReqVO pageReqVO) {
        return CatalogDbMapper.selectPage(pageReqVO);
    }


    @Override
    public Long createCatalogDb(CatalogDbSaveReqVO createReqVO) {
        CatalogDbDO CatalogDbDO = BeanUtils.toBean(createReqVO, CatalogDbDO.class);
        CatalogDbMapper.insert(CatalogDbDO);
        return CatalogDbDO.getId();
    }

    @Override
    public int updateCatalogDb(CatalogDbSaveReqVO updateReqVO) {
        // 相关校验

        // 更新数据库
        CatalogDbDO updateDbDO = BeanUtils.toBean(updateReqVO, CatalogDbDO.class);
        ForceUpdateHelper.updateById(updateDbDO, baseMapper,
                Lists.newArrayList(CatalogDbDO::getValidFlag, CatalogDbDO::getAuditTime, CatalogDbDO::getAuditStatus));
        return CatalogDbMapper.updateById(updateDbDO);
    }
    @Override
    public int removeCatalogDb(Collection<Long> idList) {
        // 批量删除数据库
        //return CatalogDbMapper.deleteBatchIds(idList);
        // 批量删除库元数据
        if (tableMapper.existsByDbIds(idList)) {
            throw new ServiceException("被表元数据引用，不可删除");
        }
        return CatalogDbMapper.delete(Wrappers.lambdaQuery(CatalogDbDO.class)
                .in(CatalogDbDO::getId, idList)
                .eq(CatalogDbDO::getStatus, "0"));
    }

    @Override
    public CatalogDbRespVO getCatalogDbById(Long id) {
       // return CatalogDbMapper.selectById(id);
        CatalogDbDO one = CatalogDbMapper.findById(id);
        CatalogDbRespVO respVO = BeanUtils.toBean(one, CatalogDbRespVO.class);
        if (respVO.getDatasourceId() != null) {
            AssetsDatasourceRespDTO datasource = daDatasourceApiService.getDatasourceById(respVO.getDatasourceId());
            respVO.setDatasource(datasource);
        }
        return respVO;
    }

    @Override
    public List<CatalogDbDO> getCatalogDbList(CatalogDbPageReqVO CatalogDb) {
       // return CatalogDbMapper.selectList();

        MPJLambdaWrapper<CatalogDbDO> wrapper = new MPJLambdaWrapper<>();
        wrapper.selectAll(CatalogDbDO.class)
                .select("t2.NAME AS sourceSystemName")
                .leftJoin("TAX_SOURCE_SYSTEM t2 on t.SOURCE_SYSTEM_ID = t2.ID AND t2.DEL_FLAG = '0'")
                .eq(CatalogDbDO::getDelFlag, "0")

                // ===== 以下都是“if 条件” =====
                .eq(CatalogDb.getId() != null, CatalogDbDO::getId, CatalogDb.getId())
                .eq(CatalogDb.getTaskId()!= null, CatalogDbDO::getTaskId, CatalogDb.getTaskId())
                .eq(CatalogDb.getDatasourceId() != null, CatalogDbDO::getDatasourceId, CatalogDb.getDatasourceId())
                .eq(StringUtils.isNotBlank(CatalogDb.getDbType()), CatalogDbDO::getDbType, CatalogDb.getDbType())
                .eq(CatalogDb.getBusinessLeader() != null, CatalogDbDO::getBusinessLeader, CatalogDb.getBusinessLeader())
                .eq(CatalogDb.getResponsibleDept() != null, CatalogDbDO::getResponsibleDept, CatalogDb.getResponsibleDept())
                .eq(StringUtils.isNotBlank(CatalogDb.getPortalVisible()), CatalogDbDO::getPortalVisible, CatalogDb.getPortalVisible())
                .eq(StringUtils.isNotBlank(CatalogDb.getSourceSystemName()), CatalogDbDO::getSourceSystemName, CatalogDb.getSourceSystemName())
                .eq(StringUtils.isNotBlank(CatalogDb.getStatus()), CatalogDbDO::getStatus, CatalogDb.getStatus())
                .eq(StringUtils.isNotBlank(CatalogDb.getAuditStatus()), CatalogDbDO::getAuditStatus, CatalogDb.getAuditStatus())
                .in(org.apache.commons.collections4.CollectionUtils.isNotEmpty(CatalogDb.getDatasourceIdList()), CatalogDbDO::getDatasourceId, CatalogDb.getDatasourceIdList())

                // like 查询
                .like(StringUtils.isNotBlank(CatalogDb.getDbName()), CatalogDbDO::getDbName, CatalogDb.getDbName())
                .like(StringUtils.isNotBlank(CatalogDb.getIp()), CatalogDbDO::getIp, CatalogDb.getIp())
                .orderByStr(org.apache.commons.lang3.StringUtils.isNotBlank(CatalogDb.getOrderByColumn()), org.apache.commons.lang3.StringUtils.equals("asc", CatalogDb.getIsAsc()), org.apache.commons.lang3.StringUtils.isNotBlank(CatalogDb.getOrderByColumn()) ? Arrays.asList(CatalogDb.getOrderByColumn().split(",")) : null);
        List<CatalogDbDO> CatalogDbDOS = CatalogDbMapper.selectList(wrapper);
        return CollectionUtils.isEmpty(CatalogDbDOS) ? new ArrayList<>() : CatalogDbDOS;

    }

    @Override
    public Map<Long, CatalogDbDO> getCatalogDbMap() {
        List<CatalogDbDO> CatalogDbList = CatalogDbMapper.selectList();
        return CatalogDbList.stream()
                .collect(Collectors.toMap(
                        CatalogDbDO::getId,
                        CatalogDbDO -> CatalogDbDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing
                ));
    }

    @Override
    public Integer toggle(Long id, String status) {
        CatalogDbDO update = new CatalogDbDO();
        update.setId(id);
        update.setStatus(status);
        return CatalogDbMapper.updateById(update);
    }

    @Override
    public Integer editPortalVisible(Long id, String portalVisible) {
        CatalogDbDO update = new CatalogDbDO();
        update.setId(id);
        update.setPortalVisible(portalVisible);
        return CatalogDbMapper.updateById(update);
    }


    @Override
    public List<CatalogDbRespVO> getCatalogDbByTaskId(Long taskId) {
        MPJLambdaWrapper<CatalogDbDO> wrapper = new MPJLambdaWrapper<>();
        wrapper.eq(CatalogDbDO::getTaskId, taskId)
                .eq(CatalogDbDO::getDelFlag, "0");

        List<CatalogDbDO> CatalogDbDOS = CatalogDbMapper.selectList(wrapper);

        return CollectionUtils.isEmpty(CatalogDbDOS) ? new ArrayList<>() : BeanUtils.toBean(CatalogDbDOS, CatalogDbRespVO.class);
    }

    @Override
    public BatchDeleteCheck<Long> batchDeleteCheck(List<Long> ids) {
        List<CatalogDbDO> list = baseMapper.selectBatchIds(ids);
        int cannotDeleteCount = 0;
        List<Long> canDeleteIds = new ArrayList<>();
        for (CatalogDbDO one : list) {
            if ("1".equals(one.getStatus())) {
                cannotDeleteCount++;
                continue;
            }
            boolean exists = tableMapper.existsByDbId(one.getId());
            if (exists) {
                cannotDeleteCount++;
            } else {
                canDeleteIds.add(one.getId());
            }
        }
        return new BatchDeleteCheck<>(cannotDeleteCount, canDeleteIds);
    }

    @Override
    public PageResult<CatalogMetaSearchRespDTO> selectMetaSearchPage(CatalogMetaSearchRespDTO req) {
        Integer offset = (req.getPageNum() - 1) * req.getPageSize();
        List<CatalogMetaSearchRespDTO> list =
                CatalogDbMapper.selectMetaSearchPage(
                        req.getKeyword(),          // 关键字
                        req.getTypes(),         // 元数据类型（多选）
                        req.getDbTypes(),       // 数据源类型（多选）
                        req.getStartTime(),     // 开始时间
                        req.getEndTime(),       // 结束时间
                        offset,
                        req.getPageSize()
                );

        for (CatalogMetaSearchRespDTO mdMetaSearchRespDTO : list) {
            String type = mdMetaSearchRespDTO.getType();
            Long id = mdMetaSearchRespDTO.getId();
            if (StringUtils.equals("1", type)) {
                mdMetaSearchRespDTO.setMdDbDO(this.getMdDbById(id));
            }
            if (StringUtils.equals("2", type)) {
                mdMetaSearchRespDTO.setMdTableRespVO(CatalogTableService.getCatalogTableById(id));
            }
            if (StringUtils.equals("3", type)) {
                mdMetaSearchRespDTO.setMdColumnDO(CatalogColumnService.getCatalogColumnById(id));
            }
        }
        Long total =
                CatalogDbMapper.selectMetaSearchCount(
                        req.getKeyword(),          // 关键字
                        req.getTypes(),
                        req.getDbTypes(),
                        req.getStartTime(),
                        req.getEndTime()
                );

        return new PageResult<>(list, total);
    }

    public CatalogDbRespVO getMdDbById(Long id) {
        CatalogDbDO one = CatalogDbMapper.findById(id);
        CatalogDbRespVO respVO = BeanUtils.toBean(one, CatalogDbRespVO.class);
        if (respVO.getDatasourceId() != null) {
            AssetsDatasourceRespDTO datasource = daDatasourceApiService.getDatasourceById(respVO.getDatasourceId());
            respVO.setDatasource(datasource);
        }
        return respVO;
    }


}
