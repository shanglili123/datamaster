package com.datamaster.module.ontology.service.impl;

import com.datamaster.common.api.space.ISpaceApi;
import com.datamaster.common.core.domain.entity.SysRole;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.security.AccessPolicy;
import com.datamaster.common.utils.SecurityUtils;
import com.datamaster.module.ontology.dal.dataobject.OntologyDO;
import com.datamaster.module.ontology.dal.mapper.OntologyMapper;
import org.springframework.stereotype.Component;
import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;

/** No owner or space-admin cross-space bypass. Always require the ontology to exist. */
@Component
public class MetricAccessGuard {
    @Resource private OntologyMapper ontologyMapper;
    @Resource private ISpaceApi spaceApi;

    public void assertOntologyAccess(Long ontologyId) {
        if (ontologyId == null) throw new ServiceException("本体ID不能为空");
        OntologyDO ontology = ontologyMapper.selectById(ontologyId);
        if (ontology == null) throw new ServiceException("本体不存在");
        Long userId = SecurityUtils.getUserId();
        List<SysRole> roles = Collections.emptyList();
        if (SecurityUtils.getLoginUser() != null && SecurityUtils.getLoginUser().getUser() != null) {
            roles = SecurityUtils.getLoginUser().getUser().getRoles();
        }
        if (AccessPolicy.isPlatformAdmin(userId, roles) || AccessPolicy.hasOpsRole(roles)) return;
        if (!spaceApi.hasUserSpace(userId, ontology.getSpaceId(), ontology.getSpaceCode())) {
            throw new ServiceException("无权访问其他空间的指标");
        }
    }
}
