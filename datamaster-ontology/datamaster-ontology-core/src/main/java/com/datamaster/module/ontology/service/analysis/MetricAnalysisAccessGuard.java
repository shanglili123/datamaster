package com.datamaster.module.ontology.service.analysis;

import com.datamaster.common.api.space.ISpaceApi;
import com.datamaster.common.utils.SecurityUtils;
import com.datamaster.module.ontology.dal.dataobject.OntologyDO;
import com.datamaster.module.ontology.dal.mapper.OntologyMapper;
import org.springframework.stereotype.Component;
import javax.annotation.Resource;
import static com.datamaster.module.ontology.service.metric.MetricRules.require;

/** Analysis deliberately has no platform-admin/ops/owner membership bypass. */
@Component
public class MetricAnalysisAccessGuard {
    @Resource private OntologyMapper ontologyMapper;
    @Resource private ISpaceApi spaceApi;
    public OntologyDO ontology(Long id) {
        require(id != null, "本体ID不能为空");
        OntologyDO ontology = ontologyMapper.selectById(id);
        require(ontology != null, "本体不存在");
        Long userId = SecurityUtils.getUserId();
        require(userId != null && spaceApi.hasUserSpace(userId, ontology.getSpaceId(), ontology.getSpaceCode()),
                "仅本体所属空间成员可使用指标分析");
        return ontology;
    }
}
