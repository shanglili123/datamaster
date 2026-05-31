package com.datamaster.module.catalog.service.columnLog.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.module.catalog.controller.admin.columnLog.vo.CatalogColumnLogPageReqVO;
import com.datamaster.module.catalog.controller.admin.columnLog.vo.CatalogColumnLogSaveReqVO;
import com.datamaster.module.catalog.dal.dataobject.columnLog.CatalogColumnLogDO;
import com.datamaster.module.catalog.dal.dataobject.metadata.CatalogColumnDO;
import com.datamaster.module.catalog.dal.mapper.columnLog.CatalogColumnLogMapper;
import com.datamaster.module.catalog.service.columnLog.ICatalogColumnLogService;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 元数据字段信息 - 日志Service业务层处理
 *
 * @author qdata
 * @date 2026-03-10
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
@RequiredArgsConstructor
public class CatalogColumnLogServiceImpl  extends ServiceImpl<CatalogColumnLogMapper,CatalogColumnLogDO> implements ICatalogColumnLogService {
    private final CatalogColumnLogMapper mapper;
    @Resource
    private CatalogColumnLogMapper CatalogColumnLogMapper;


    @Override
    public PageResult<CatalogColumnLogDO> getCatalogColumnLogPage(CatalogColumnLogPageReqVO pageReqVO) {
        return CatalogColumnLogMapper.selectPage(pageReqVO);
    }

    @Override
    public Long createCatalogColumnLog(CatalogColumnLogSaveReqVO createReqVO) {
        CatalogColumnLogDO CatalogColumnLogDO = BeanUtils.toBean(createReqVO, CatalogColumnLogDO.class);
        CatalogColumnLogMapper.insert(CatalogColumnLogDO);
        return CatalogColumnLogDO.getId();
    }

    @Override
    public Long createCatalogColumnLog(List<CatalogColumnDO> columnDOList) {
        List<CatalogColumnLogDO> CatalogColumnLogDOS = this.convertCatalogColumnLogDO(columnDOList);
        mapper.insertBatch(CatalogColumnLogDOS);
        return 1L;
    }

    @Override
    public int updateCatalogColumnLog(CatalogColumnLogSaveReqVO updateReqVO) {
        // 相关校验

        // 更新元数据字段信息 - 日志
        CatalogColumnLogDO updateCatalogColumnLogDO = BeanUtils.toBean(updateReqVO, CatalogColumnLogDO.class);
        return CatalogColumnLogMapper.updateById(updateCatalogColumnLogDO);
    }
    @Override
    public int removeCatalogColumnLog(Collection<Long> idList) {
        // 批量删除元数据字段信息 - 日志
        return CatalogColumnLogMapper.deleteBatchIds(idList);
    }

    @Override
    public CatalogColumnLogDO getCatalogColumnLogById(Long id) {
        return CatalogColumnLogMapper.selectById(id);
    }

    @Override
    public List<CatalogColumnLogDO> getCatalogColumnLogList() {
        return CatalogColumnLogMapper.selectList();
    }

    @Override
    public Map<Long, CatalogColumnLogDO> getCatalogColumnLogMap() {
        List<CatalogColumnLogDO> CatalogColumnLogList = CatalogColumnLogMapper.selectList();
        return CatalogColumnLogList.stream()
                .collect(Collectors.toMap(
                        CatalogColumnLogDO::getId,
                        CatalogColumnLogDO -> CatalogColumnLogDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing
                ));
    }

    public List<CatalogColumnLogDO> convertCatalogColumnLogDO(List<CatalogColumnDO> CatalogColumnReqDTOList){
        List<CatalogColumnLogDO> result = new ArrayList<>();
        for (CatalogColumnDO CatalogColumnSaveReqVO : CatalogColumnReqDTOList) {
            CatalogColumnLogDO columnDO = BeanUtils.toBean(CatalogColumnSaveReqVO, CatalogColumnLogDO.class);
            columnDO.setColumnId(CatalogColumnSaveReqVO.getId());
            columnDO.setTableId(CatalogColumnSaveReqVO.getCatalogTableLogId());
            columnDO.setId(null);
            result.add(columnDO);
        }
        return result;
    }

}
