package com.datamaster.module.taxonomy.api.sourceSystem;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.module.taxonomy.api.sourceSystem.dto.TaxonomySourceSystemRespDTO;
import com.datamaster.module.taxonomy.api.sourceSystem.service.ITaxonomySourceSystemApiService;
import com.datamaster.module.taxonomy.dal.dataobject.sourceSystem.TaxonomySourceSystemDO;
import com.datamaster.module.taxonomy.service.sourceSystem.ITaxonomySourceSystemService;

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
public class TaxonomySourceSystemApiImpl implements ITaxonomySourceSystemApiService {

    @Resource
    private ITaxonomySourceSystemService TaxonomySourceSystemService;

    @Override
    public List<TaxonomySourceSystemRespDTO> getValidSourceSystems() {
        return getValidSourceSystems(null);
    }

    @Override
    public List<TaxonomySourceSystemRespDTO> getValidSourceSystems(Long spaceId) {
        List<TaxonomySourceSystemDO> validSourceSystems = TaxonomySourceSystemService.getValidSourceSystemList(spaceId);
        return validSourceSystems.stream()
                .map(sourceSystem -> BeanUtils.toBean(sourceSystem, TaxonomySourceSystemRespDTO.class))
                .collect(Collectors.toList());
    }
}
