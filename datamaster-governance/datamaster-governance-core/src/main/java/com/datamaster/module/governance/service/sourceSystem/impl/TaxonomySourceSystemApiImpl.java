package com.datamaster.module.governance.service.sourceSystem.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.datamaster.common.api.source.ISourceSystemApi;
import com.datamaster.common.api.source.dto.SourceSystemRespDTO;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.module.governance.dal.dataobject.sourceSystem.TaxonomySourceSystemDO;
import com.datamaster.module.governance.service.sourceSystem.ITaxonomySourceSystemService;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 来源系统 API 实现类
 *
 * @author DATAMASTER
 * @date 2026-04-30
 */
@Slf4j
@Service
public class TaxonomySourceSystemApiImpl implements ISourceSystemApi {

    @Resource
    private ITaxonomySourceSystemService TaxonomySourceSystemService;

    @Override
    public List<SourceSystemRespDTO> getValidSourceSystems() {
        return getValidSourceSystems(null);
    }

    @Override
    public List<SourceSystemRespDTO> getValidSourceSystems(Long spaceId) {
        List<TaxonomySourceSystemDO> validSourceSystems = TaxonomySourceSystemService.getValidSourceSystemList(spaceId);
        return validSourceSystems.stream()
                .map(sourceSystem -> BeanUtils.toBean(sourceSystem, SourceSystemRespDTO.class))
                .collect(Collectors.toList());
    }
}
