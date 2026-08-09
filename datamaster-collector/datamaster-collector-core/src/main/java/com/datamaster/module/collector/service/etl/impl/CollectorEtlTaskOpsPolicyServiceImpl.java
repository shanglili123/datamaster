package com.datamaster.module.collector.service.etl.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlTaskOpsPolicySaveReqVO;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlTaskDO;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlTaskOpsPolicyDO;
import com.datamaster.module.collector.dal.mapper.etl.CollectorEtlTaskMapper;
import com.datamaster.module.collector.dal.mapper.etl.CollectorEtlTaskOpsPolicyMapper;
import com.datamaster.module.collector.service.etl.ICollectorEtlTaskOpsPolicyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service
@Transactional(rollbackFor = Exception.class)
public class CollectorEtlTaskOpsPolicyServiceImpl extends ServiceImpl<CollectorEtlTaskOpsPolicyMapper, CollectorEtlTaskOpsPolicyDO>
        implements ICollectorEtlTaskOpsPolicyService {

    @Resource
    private CollectorEtlTaskMapper collectorEtlTaskMapper;

    @Override
    public CollectorEtlTaskOpsPolicyDO getByTaskId(Long taskId) {
        if (taskId == null) {
            return null;
        }
        return getOne(Wrappers.lambdaQuery(CollectorEtlTaskOpsPolicyDO.class)
                .eq(CollectorEtlTaskOpsPolicyDO::getTaskId, taskId), false);
    }

    @Override
    public CollectorEtlTaskOpsPolicyDO saveOrUpdatePolicy(CollectorEtlTaskOpsPolicySaveReqVO reqVO) {
        if (reqVO == null || reqVO.getTaskId() == null) {
            throw new ServiceException("任务ID不能为空");
        }
        CollectorEtlTaskDO task = collectorEtlTaskMapper.selectById(reqVO.getTaskId());
        if (task == null) {
            throw new ServiceException("任务不存在");
        }
        CollectorEtlTaskOpsPolicyDO policy = getByTaskId(reqVO.getTaskId());
        if (policy == null) {
            policy = new CollectorEtlTaskOpsPolicyDO();
            policy.setTaskId(task.getId());
            policy.setTaskType(task.getType());
            policy.setValidFlag(Boolean.TRUE);
            policy.setDelFlag(Boolean.FALSE);
        }
        policy.setFailStopEnabled(Boolean.TRUE.equals(reqVO.getFailStopEnabled()));
        policy.setAiManaged(Boolean.TRUE.equals(reqVO.getAiManaged()));
        policy.setAutoRecoverEnabled(Boolean.TRUE.equals(reqVO.getAutoRecoverEnabled()));
        policy.setMaxRecoverTimes(reqVO.getMaxRecoverTimes() == null ? 1 : Math.max(0, reqVO.getMaxRecoverTimes()));
        policy.setRecoverStrategy(StringUtils.defaultIfBlank(reqVO.getRecoverStrategy(), "SAFE_AUTO"));
        policy.setNotifyUsers(reqVO.getNotifyUsers());
        saveOrUpdate(policy);
        return policy;
    }
}
