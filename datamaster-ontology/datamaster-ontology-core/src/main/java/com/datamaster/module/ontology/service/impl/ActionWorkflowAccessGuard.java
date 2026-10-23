package com.datamaster.module.ontology.service.impl;

import com.datamaster.common.api.space.ISpaceApi;
import com.datamaster.common.core.domain.entity.SysRole;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.security.AccessPolicy;
import com.datamaster.common.utils.SecurityUtils;
import com.datamaster.module.ontology.dal.dataobject.ActionWorkflowDO;
import com.datamaster.module.ontology.dal.dataobject.OntologyDO;
import com.datamaster.module.ontology.dal.mapper.OntologyMapper;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Workflow 定义和运行共用的空间访问守卫。 */
@Component
public class ActionWorkflowAccessGuard {

    @Resource
    private OntologyMapper ontologyMapper;

    @Resource
    private ISpaceApi spaceApi;

    public void assertWorkflowAccess(ActionWorkflowDO workflow) {
        if (workflow == null) throw new ServiceException("Workflow 不存在");
        Long userId = SecurityUtils.getUserId();
        if (isPrivileged(userId)) return;
        OntologyDO ontology = ontologyMapper.selectById(workflow.getOntologyId());
        boolean member = isSpaceMember(userId, ontology);
        boolean owner = Objects.equals(workflow.getCreatorId(), userId);
        if (!member && !owner) {
            throw new ServiceException("无权访问其他空间的 Workflow");
        }
    }

    public void assertOntologyAccess(Long ontologyId) {
        Long userId = SecurityUtils.getUserId();
        if (isPrivileged(userId)) return;
        OntologyDO ontology = ontologyMapper.selectById(ontologyId);
        if (!isSpaceMember(userId, ontology)) {
            throw new ServiceException("无权在其他空间创建或修改 Workflow");
        }
    }

    private boolean isSpaceMember(Long userId, OntologyDO ontology) {
        return ontology != null
                && spaceApi.hasUserSpace(userId, ontology.getSpaceId(), ontology.getSpaceCode());
    }

    private boolean isPrivileged(Long userId) {
        List<SysRole> roles = Collections.emptyList();
        try {
            if (SecurityUtils.getLoginUser() != null && SecurityUtils.getLoginUser().getUser() != null) {
                roles = SecurityUtils.getLoginUser().getUser().getRoles();
            }
        } catch (Exception ignored) {
            // getUserId() 在未登录时已经抛出未授权异常。
        }
        return AccessPolicy.isPlatformAdmin(userId, roles)
                || AccessPolicy.hasSpaceAdminRole(roles)
                || AccessPolicy.hasOpsRole(roles);
    }
}
