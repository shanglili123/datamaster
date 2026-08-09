package com.datamaster.metadata.service.task.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.datamaster.metadata.controller.admin.task.vo.CatalogTaskQualityRespVO;
import com.datamaster.metadata.dal.dataobject.qa.QualityTaskDO;
import com.datamaster.metadata.dal.dataobject.qa.QualityTaskEvaluateDO;
import com.datamaster.metadata.dal.dataobject.qa.QualityTaskObjDO;
import com.datamaster.metadata.dal.dataobject.task.CatalogTaskQualityDO;
import com.datamaster.metadata.dal.mapper.qa.QualityTaskEvaluateMapper;
import com.datamaster.metadata.dal.mapper.qa.QualityTaskMapper;
import com.datamaster.metadata.dal.mapper.qa.QualityTaskObjMapper;
import com.datamaster.metadata.dal.mapper.task.CatalogTaskQualityMapper;
import com.datamaster.metadata.service.task.ICatalogTaskQualityService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 探查任务-质量探查任务关联 Service 实现
 *
 * @author DATAMASTER
 * @date 2026-07-31
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class CatalogTaskQualityServiceImpl extends ServiceImpl<CatalogTaskQualityMapper, CatalogTaskQualityDO>
        implements ICatalogTaskQualityService {

    @Resource
    private QualityTaskMapper qualityTaskMapper;

    @Resource
    private QualityTaskObjMapper qualityTaskObjMapper;

    @Resource
    private QualityTaskEvaluateMapper qualityTaskEvaluateMapper;

    @Override
    public List<CatalogTaskQualityRespVO> getQualityListByCatTaskId(Long catTaskId) {
        List<CatalogTaskQualityDO> relations = list(Wrappers.lambdaQuery(CatalogTaskQualityDO.class)
                .eq(CatalogTaskQualityDO::getCatTaskId, catTaskId));
        if (CollectionUtils.isEmpty(relations)) {
            return Collections.emptyList();
        }
        Set<Long> qualityTaskIds = relations.stream()
                .map(CatalogTaskQualityDO::getQualityTaskId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, QualityTaskDO> qualityTaskMap = qualityTaskMapper.selectBatchIds(qualityTaskIds).stream()
                .collect(Collectors.toMap(QualityTaskDO::getId, t -> t, (a, b) -> a));

        Map<Long, Long> objCountMap = qualityTaskObjMapper.selectList(Wrappers.lambdaQuery(QualityTaskObjDO.class)
                .in(QualityTaskObjDO::getTaskId, qualityTaskIds)).stream()
                .collect(Collectors.groupingBy(QualityTaskObjDO::getTaskId, Collectors.counting()));
        Map<Long, Long> evaluateCountMap = qualityTaskEvaluateMapper.selectList(Wrappers.lambdaQuery(QualityTaskEvaluateDO.class)
                .in(QualityTaskEvaluateDO::getTaskId, qualityTaskIds)).stream()
                .collect(Collectors.groupingBy(QualityTaskEvaluateDO::getTaskId, Collectors.counting()));

        List<CatalogTaskQualityRespVO> result = new ArrayList<>();
        for (CatalogTaskQualityDO relation : relations) {
            QualityTaskDO qualityTask = qualityTaskMap.get(relation.getQualityTaskId());
            CatalogTaskQualityRespVO respVO = new CatalogTaskQualityRespVO();
            respVO.setId(relation.getId());
            respVO.setCatTaskId(relation.getCatTaskId());
            respVO.setQualityTaskId(relation.getQualityTaskId());
            respVO.setCreateBy(relation.getCreateBy());
            respVO.setCreateTime(relation.getCreateTime());
            if (qualityTask != null) {
                respVO.setQualityTaskName(qualityTask.getTaskName());
                respVO.setQualityTaskStatus(qualityTask.getStatus());
                respVO.setQualityTaskCycle(qualityTask.getCycle());
                Long objNum = objCountMap.get(qualityTask.getId());
                Long evaluateNum = evaluateCountMap.get(qualityTask.getId());
                respVO.setQualityTaskObjNum(objNum == null ? 0 : objNum.intValue());
                respVO.setQualityTaskEvaluateNum(evaluateNum == null ? 0 : evaluateNum.intValue());
            }
            result.add(respVO);
        }
        return result;
    }

    @Override
    public void bindQualityTask(Long catTaskId, Long qualityTaskId) {
        if (catTaskId == null || qualityTaskId == null) {
            throw new IllegalArgumentException("探查任务ID和质量探查任务ID不能为空");
        }
        Long count = count(Wrappers.lambdaQuery(CatalogTaskQualityDO.class)
                .eq(CatalogTaskQualityDO::getCatTaskId, catTaskId)
                .eq(CatalogTaskQualityDO::getQualityTaskId, qualityTaskId));
        if (count != null && count > 0) {
            return;
        }
        CatalogTaskQualityDO relation = new CatalogTaskQualityDO();
        relation.setCatTaskId(catTaskId);
        relation.setQualityTaskId(qualityTaskId);
        save(relation);
    }

    @Override
    public void unbindQualityTask(Long id) {
        removeById(id);
    }

    @Override
    public List<Long> getQualityTaskIdListByCatTaskId(Long catTaskId) {
        List<CatalogTaskQualityDO> relations = list(Wrappers.lambdaQuery(CatalogTaskQualityDO.class)
                .eq(CatalogTaskQualityDO::getCatTaskId, catTaskId));
        if (CollectionUtils.isEmpty(relations)) {
            return Collections.emptyList();
        }
        return relations.stream()
                .map(CatalogTaskQualityDO::getQualityTaskId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public void removeByCatTaskIds(Collection<Long> catTaskIdList) {
        if (CollectionUtils.isEmpty(catTaskIdList)) {
            return;
        }
        remove(Wrappers.lambdaQuery(CatalogTaskQualityDO.class)
                .in(CatalogTaskQualityDO::getCatTaskId, catTaskIdList));
    }
}
