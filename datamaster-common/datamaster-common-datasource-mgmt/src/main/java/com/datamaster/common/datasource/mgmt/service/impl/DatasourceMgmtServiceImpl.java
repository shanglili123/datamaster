package com.datamaster.common.datasource.mgmt.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.datamaster.common.core.domain.entity.DatasourceDO;
import com.datamaster.common.core.page.PageParam;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.datasource.mgmt.mapper.DatasourceMgmtMapper;
import com.datamaster.common.datasource.mgmt.service.IDatasourceMgmtService;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(rollbackFor = Exception.class)
public class DatasourceMgmtServiceImpl extends ServiceImpl<DatasourceMgmtMapper, DatasourceDO> implements IDatasourceMgmtService {

    @Resource
    private DatasourceMgmtMapper datasourceMgmtMapper;

    @Override
    public PageResult<DatasourceDO> getDatasourcePage(PageParam pageParam, LambdaQueryWrapperX<DatasourceDO> queryWrapper) {
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));
        if (queryWrapper == null) {
            queryWrapper = new LambdaQueryWrapperX<>();
        }
        queryWrapper.orderBy(pageParam.getOrderByColumn(), pageParam.getIsAsc(), allowedColumns);
        return datasourceMgmtMapper.selectPage(pageParam, queryWrapper);
    }

    @Override
    public DatasourceDO getDatasourceDOById(Long id) {
        return datasourceMgmtMapper.selectById(id);
    }

    @Override
    public List<DatasourceDO> getDatasourceList() {
        return datasourceMgmtMapper.selectList();
    }

    @Override
    public Map<Long, DatasourceDO> getDatasourceMap() {
        List<DatasourceDO> list = datasourceMgmtMapper.selectList();
        return list.stream().collect(Collectors.toMap(DatasourceDO::getId, d -> d, (existing, replacement) -> existing));
    }

    @Override
    public Long createDatasource(DatasourceDO datasource) {
        datasourceMgmtMapper.insert(datasource);
        return datasource.getId();
    }

    @Override
    public int updateDatasource(DatasourceDO datasource) {
        return datasourceMgmtMapper.updateById(datasource);
    }

    @Override
    public int removeDatasource(Collection<Long> idList) {
        return datasourceMgmtMapper.deleteBatchIds(idList);
    }

    @Override
    public Boolean editDatasourceStatus(Long datasourceId, Boolean status) {
        return update(Wrappers.lambdaUpdate(DatasourceDO.class)
                .eq(DatasourceDO::getId, datasourceId)
                .set(DatasourceDO::getValidFlag, status));
    }
}
