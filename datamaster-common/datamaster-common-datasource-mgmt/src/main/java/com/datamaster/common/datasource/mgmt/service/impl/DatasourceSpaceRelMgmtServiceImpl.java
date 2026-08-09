package com.datamaster.common.datasource.mgmt.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.datamaster.common.core.domain.entity.DatasourceSpaceRelDO;
import com.datamaster.common.datasource.mgmt.mapper.DatasourceSpaceRelMgmtMapper;
import com.datamaster.common.datasource.mgmt.service.IDatasourceSpaceRelMgmtService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.List;

@Service
@Transactional(rollbackFor = Exception.class)
public class DatasourceSpaceRelMgmtServiceImpl extends ServiceImpl<DatasourceSpaceRelMgmtMapper, DatasourceSpaceRelDO> implements IDatasourceSpaceRelMgmtService {

    @Resource
    private DatasourceSpaceRelMgmtMapper datasourceSpaceRelMgmtMapper;

    @Override
    public Long createDatasourceSpaceRel(DatasourceSpaceRelDO rel) {
        datasourceSpaceRelMgmtMapper.insert(rel);
        return rel.getId();
    }

    @Override
    public int updateDatasourceSpaceRel(DatasourceSpaceRelDO rel) {
        return datasourceSpaceRelMgmtMapper.updateById(rel);
    }

    @Override
    public int removeDatasourceSpaceRel(Collection<Long> idList) {
        return datasourceSpaceRelMgmtMapper.deleteBatchIds(idList);
    }

    @Override
    public DatasourceSpaceRelDO getDatasourceSpaceRelById(Long id) {
        return datasourceSpaceRelMgmtMapper.selectById(id);
    }

    @Override
    public List<DatasourceSpaceRelDO> getDatasourceSpaceRelList() {
        return datasourceSpaceRelMgmtMapper.selectList();
    }

    @Override
    public List<DatasourceSpaceRelDO> getDatasourceSpaceRelList(DatasourceSpaceRelDO condition) {
        LambdaQueryWrapper<DatasourceSpaceRelDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(condition.getDatasourceId() != null, DatasourceSpaceRelDO::getDatasourceId, condition.getDatasourceId());
        queryWrapper.eq(condition.getSpaceId() != null, DatasourceSpaceRelDO::getSpaceId, condition.getSpaceId());
        return datasourceSpaceRelMgmtMapper.selectList(queryWrapper);
    }
}
