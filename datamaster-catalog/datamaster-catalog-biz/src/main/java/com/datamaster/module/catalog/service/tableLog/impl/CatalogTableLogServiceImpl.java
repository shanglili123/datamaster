package com.datamaster.module.catalog.service.tableLog.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.module.catalog.controller.admin.metadata.vo.CatalogTableSaveReqVO;
import com.datamaster.module.catalog.controller.admin.tableLog.vo.CatalogTableLogPageReqVO;
import com.datamaster.module.catalog.controller.admin.tableLog.vo.CatalogTableLogSaveReqVO;
import com.datamaster.module.catalog.dal.dataobject.tableLog.CatalogTableLogDO;
import com.datamaster.module.catalog.dal.mapper.tableLog.CatalogTableLogMapper;
import com.datamaster.module.catalog.service.tableLog.ICatalogTableLogService;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 元数据信息 - 日志Service业务层处理
 *
 * @author qdata
 * @date 2026-03-10
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class CatalogTableLogServiceImpl  extends ServiceImpl<CatalogTableLogMapper,CatalogTableLogDO> implements ICatalogTableLogService {
    @Resource
    private CatalogTableLogMapper CatalogTableLogMapper;

    @Override
    public PageResult<CatalogTableLogDO> getCatalogTableLogPage(CatalogTableLogPageReqVO pageReqVO) {
        return CatalogTableLogMapper.selectPage(pageReqVO);
    }

    @Override
    public Long createCatalogTableLog(CatalogTableLogSaveReqVO createReqVO) {
        CatalogTableLogDO CatalogTableLogDO = BeanUtils.toBean(createReqVO, CatalogTableLogDO.class);
        CatalogTableLogDO.setVersion(getVersion());
        CatalogTableLogMapper.insert(CatalogTableLogDO);
        return CatalogTableLogDO.getId();
    }

    @Override
    public int updateCatalogTableLog(CatalogTableLogSaveReqVO updateReqVO) {
        // 相关校验

        // 更新元数据信息 - 日志
        CatalogTableLogDO updateCatalogTableLogDO = BeanUtils.toBean(updateReqVO, CatalogTableLogDO.class);
        return CatalogTableLogMapper.updateById(updateCatalogTableLogDO);
    }
    @Override
    public int removeCatalogTableLog(Collection<Long> idList) {
        // 批量删除元数据信息 - 日志
        return CatalogTableLogMapper.deleteBatchIds(idList);
    }

    @Override
    public CatalogTableLogDO getCatalogTableLogById(Long id) {
        return CatalogTableLogMapper.selectById(id);
    }

    @Override
    public List<CatalogTableLogDO> getCatalogTableLogList() {
        return CatalogTableLogMapper.selectList();
    }

    /**
     * 根据元数据表信息添加元数据版本变更日志
     * @param table 元数据表信息
     * @return 元数据版本变更日志id
     */
    @Override
    public Long createCatalogTableLog(CatalogTableSaveReqVO table) {
        CatalogTableLogSaveReqVO CatalogTableLogSaveReqVO = this.convertCatalogTableLogSaveReqVO(table);
        return this.createCatalogTableLog(CatalogTableLogSaveReqVO);
    }

    @Override
    public Map<Long, CatalogTableLogDO> getCatalogTableLogMap() {
        List<CatalogTableLogDO> CatalogTableLogList = CatalogTableLogMapper.selectList();
        return CatalogTableLogList.stream()
                .collect(Collectors.toMap(
                        CatalogTableLogDO::getId,
                        CatalogTableLogDO -> CatalogTableLogDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing
                ));
    }

    /**
     * 根据时间生成版本号 Vyyyy.MM.ddHHmm
     * @return 版本号
     */
    public  String getVersion(){
        return "V" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy.MM.ddHHmm"));
    }


    public CatalogTableLogSaveReqVO convertCatalogTableLogSaveReqVO(CatalogTableSaveReqVO CatalogTableLogDO){
        CatalogTableLogSaveReqVO bean = BeanUtils.toBean(CatalogTableLogDO, CatalogTableLogSaveReqVO.class);
        bean.setTableId(CatalogTableLogDO.getId());
        bean.setId(null);
        return bean;
    }

}
