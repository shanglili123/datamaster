package com.datamaster.module.ai.service.dbgpt;

import com.datamaster.common.core.domain.AjaxResult;

public interface IDbGptSkillSyncService {

    AjaxResult syncAllSkills();

    AjaxResult syncSkillsByDatasourceId(Long datasourceId);

    AjaxResult syncSkillById(Long skillId);
}

