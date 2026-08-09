package com.datamaster.metadata.service.task;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.metadata.controller.admin.task.vo.CatalogTaskQualityRespVO;
import com.datamaster.metadata.dal.dataobject.task.CatalogTaskQualityDO;

import java.util.Collection;
import java.util.List;

/**
 * 探查任务-质量探查任务关联 Service
 *
 * @author DATAMASTER
 * @date 2026-07-31
 */
public interface ICatalogTaskQualityService extends IService<CatalogTaskQualityDO> {

    /**
     * 查询探查任务已绑定的质量探查任务列表
     *
     * @param catTaskId 探查任务ID
     * @return 质量探查任务列表
     */
    List<CatalogTaskQualityRespVO> getQualityListByCatTaskId(Long catTaskId);

    /**
     * 绑定质量探查任务
     *
     * @param catTaskId     探查任务ID
     * @param qualityTaskId 质量探查任务ID
     */
    void bindQualityTask(Long catTaskId, Long qualityTaskId);

    /**
     * 解绑质量探查任务
     *
     * @param id 关联ID
     */
    void unbindQualityTask(Long id);

    /**
     * 查询探查任务绑定的质量探查任务ID列表
     *
     * @param catTaskId 探查任务ID
     * @return 质量探查任务ID列表
     */
    List<Long> getQualityTaskIdListByCatTaskId(Long catTaskId);

    /**
     * 删除探查任务的关联记录
     *
     * @param catTaskIdList 探查任务ID集合
     */
    void removeByCatTaskIds(Collection<Long> catTaskIdList);
}
