package com.datamaster.module.catalog.service.tableColumnRelLog.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.module.catalog.controller.admin.tableColumnRelLog.vo.CatalogTableColumnRelLogPageReqVO;
import com.datamaster.module.catalog.controller.admin.tableColumnRelLog.vo.CatalogTableColumnRelLogSaveReqVO;
import com.datamaster.module.catalog.dal.dataobject.tableColumnRelLog.CatalogTableColumnRelLogDO;
import com.datamaster.module.catalog.dal.mapper.tableColumnRelLog.CatalogTableColumnRelLogMapper;
import com.datamaster.module.catalog.service.tableColumnRelLog.ICatalogTableColumnRelLogService;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 元数据数据库与信息及字段信息关系-日志Service业务层处理
 *
 * @author qdata
 * @date 2026-03-10
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class CatalogTableColumnRelLogServiceImpl  extends ServiceImpl<CatalogTableColumnRelLogMapper,CatalogTableColumnRelLogDO> implements ICatalogTableColumnRelLogService {
    @Resource
    private CatalogTableColumnRelLogMapper CatalogTableColumnRelLogMapper;

    @Override
    public PageResult<CatalogTableColumnRelLogDO> getCatalogTableColumnRelLogPage(CatalogTableColumnRelLogPageReqVO pageReqVO) {
        return CatalogTableColumnRelLogMapper.selectPage(pageReqVO);
    }

    @Override
    public Long createCatalogTableColumnRelLog(CatalogTableColumnRelLogSaveReqVO createReqVO) {
        CatalogTableColumnRelLogDO CatalogTableColumnRelLogDO = BeanUtils.toBean(createReqVO, CatalogTableColumnRelLogDO.class);
        CatalogTableColumnRelLogMapper.insert(CatalogTableColumnRelLogDO);
        return CatalogTableColumnRelLogDO.getId();
    }

    @Override
    public int updateCatalogTableColumnRelLog(CatalogTableColumnRelLogSaveReqVO updateReqVO) {
        // 相关校验

        // 更新元数据数据库与信息及字段信息关系-日志
        CatalogTableColumnRelLogDO updateCatalogTableColumnRelLogDO = BeanUtils.toBean(updateReqVO, CatalogTableColumnRelLogDO.class);
        return CatalogTableColumnRelLogMapper.updateById(updateCatalogTableColumnRelLogDO);
    }
    @Override
    public int removeCatalogTableColumnRelLog(Collection<Long> idList) {
        // 批量删除元数据数据库与信息及字段信息关系-日志
        return CatalogTableColumnRelLogMapper.deleteBatchIds(idList);
    }

    @Override
    public CatalogTableColumnRelLogDO getCatalogTableColumnRelLogById(Long id) {
        return CatalogTableColumnRelLogMapper.selectById(id);
    }

    @Override
    public List<CatalogTableColumnRelLogDO> getCatalogTableColumnRelLogList() {
        return CatalogTableColumnRelLogMapper.selectList();
    }

    @Override
    public Map<Long, CatalogTableColumnRelLogDO> getCatalogTableColumnRelLogMap() {
        List<CatalogTableColumnRelLogDO> CatalogTableColumnRelLogList = CatalogTableColumnRelLogMapper.selectList();
        return CatalogTableColumnRelLogList.stream()
                .collect(Collectors.toMap(
                        CatalogTableColumnRelLogDO::getId,
                        CatalogTableColumnRelLogDO -> CatalogTableColumnRelLogDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing
                ));
    }


}
