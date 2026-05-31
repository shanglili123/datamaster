package com.datamaster.module.assets.service.discovery.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.module.assets.controller.admin.discovery.vo.AssetsDiscoveryTaskLogPageReqVO;
import com.datamaster.module.assets.controller.admin.discovery.vo.AssetsDiscoveryTaskLogRespVO;
import com.datamaster.module.assets.controller.admin.discovery.vo.AssetsDiscoveryTaskLogSaveReqVO;
import com.datamaster.module.assets.dal.dataobject.discovery.AssetsDiscoveryTaskLogDO;
import com.datamaster.module.assets.dal.mapper.discovery.AssetsDiscoveryTaskLogMapper;
import com.datamaster.module.assets.service.discovery.IAssetsDiscoveryLogBodyService;
import com.datamaster.module.assets.service.discovery.IAssetsDiscoveryTaskLogService;
import com.datamaster.redis.service.IRedisService;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service
 *
 * @author qdata
 * @date 2025-02-17
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class AssetsDiscoveryTaskLogServiceImpl  extends ServiceImpl<AssetsDiscoveryTaskLogMapper,AssetsDiscoveryTaskLogDO> implements IAssetsDiscoveryTaskLogService {
    @Resource
    private AssetsDiscoveryTaskLogMapper AssetsDiscoveryTaskLogMapper;
    @Resource
    private IAssetsDiscoveryLogBodyService IAssetsDiscoveryLogBodyService;
    @Resource
    @Lazy
    private IRedisService redisService;

    @Override
    public PageResult<AssetsDiscoveryTaskLogDO> getDaDiscoveryTaskLogPage(AssetsDiscoveryTaskLogPageReqVO pageReqVO) {
        return AssetsDiscoveryTaskLogMapper.selectPage(pageReqVO);
    }

    @Override
    public Long createDaDiscoveryTaskLog(AssetsDiscoveryTaskLogSaveReqVO createReqVO) {
        AssetsDiscoveryTaskLogDO dictType = BeanUtils.toBean(createReqVO, AssetsDiscoveryTaskLogDO.class);
        AssetsDiscoveryTaskLogMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public int updateDaDiscoveryTaskLog(AssetsDiscoveryTaskLogSaveReqVO updateReqVO) {
        // 相关校验

        // 更新数据发现任务日志
        AssetsDiscoveryTaskLogDO updateObj = BeanUtils.toBean(updateReqVO, AssetsDiscoveryTaskLogDO.class);
        return AssetsDiscoveryTaskLogMapper.updateById(updateObj);
    }
    @Override
    public int removeDaDiscoveryTaskLog(Collection<Long> idList) {
        // 批量删除数据发现任务日志
        int i = AssetsDiscoveryTaskLogMapper.deleteBatchIds(idList);
        for (Long id : idList) {
            IAssetsDiscoveryLogBodyService.deleteByPk(id);
        }
        return 1;
    }

    @Override
    public AssetsDiscoveryTaskLogDO getDaDiscoveryTaskLogById(Long id) {
        return AssetsDiscoveryTaskLogMapper.selectById(id);
    }

    @Override
    public List<AssetsDiscoveryTaskLogDO> getDaDiscoveryTaskLogList() {
        return AssetsDiscoveryTaskLogMapper.selectList();
    }

    @Override
    public Map<Long, AssetsDiscoveryTaskLogDO> getDaDiscoveryTaskLogMap() {
        List<AssetsDiscoveryTaskLogDO> AssetsDiscoveryTaskLogList = AssetsDiscoveryTaskLogMapper.selectList();
        return AssetsDiscoveryTaskLogList.stream()
                .collect(Collectors.toMap(
                        AssetsDiscoveryTaskLogDO::getId,
                        AssetsDiscoveryTaskLogDO -> AssetsDiscoveryTaskLogDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing
                ));
    }

        /**
         *
         *
         * @param importExcelList
         * @param isUpdateSupport
         * @param operName
         * @return
         */
        @Override
        public String importDaDiscoveryTaskLog(List<AssetsDiscoveryTaskLogRespVO> importExcelList, boolean isUpdateSupport, String operName) {
            if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
                throw new ServiceException("");
            }

            int successNum = 0;
            int failureNum = 0;
            List<String> successMessages = new ArrayList<>();
            List<String> failureMessages = new ArrayList<>();

            for (AssetsDiscoveryTaskLogRespVO respVO : importExcelList) {
                try {
                    AssetsDiscoveryTaskLogDO AssetsDiscoveryTaskLogDO = BeanUtils.toBean(respVO, AssetsDiscoveryTaskLogDO.class);
                    Long AssetsDiscoveryTaskLogId = respVO.getId();
                    if (isUpdateSupport) {
                        if (AssetsDiscoveryTaskLogId != null) {
                            AssetsDiscoveryTaskLogDO existingDaDiscoveryTaskLog = AssetsDiscoveryTaskLogMapper.selectById(AssetsDiscoveryTaskLogId);
                            if (existingDaDiscoveryTaskLog != null) {
                                AssetsDiscoveryTaskLogMapper.updateById(AssetsDiscoveryTaskLogDO);
                                successNum++;
                                successMessages.add("ID " + AssetsDiscoveryTaskLogId + " ");
                            } else {
                                failureNum++;
                                failureMessages.add("ID " + AssetsDiscoveryTaskLogId + " ");
                            }
                        } else {
                            failureNum++;
                            failureMessages.add("ID");
                        }
                    } else {
                        QueryWrapper<AssetsDiscoveryTaskLogDO> queryWrapper = new QueryWrapper<>();
                        queryWrapper.eq("id", AssetsDiscoveryTaskLogId);
                        AssetsDiscoveryTaskLogDO existingDaDiscoveryTaskLog = AssetsDiscoveryTaskLogMapper.selectOne(queryWrapper);
                        if (existingDaDiscoveryTaskLog == null) {
                            AssetsDiscoveryTaskLogMapper.insert(AssetsDiscoveryTaskLogDO);
                            successNum++;
                            successMessages.add("ID " + AssetsDiscoveryTaskLogId + " ");
                        } else {
                            failureNum++;
                            failureMessages.add("ID " + AssetsDiscoveryTaskLogId + " ");
                        }
                    }
                } catch (Exception e) {
                    failureNum++;
                    String errorMsg = "" + e.getMessage();
                    failureMessages.add(errorMsg);
                    log.error(errorMsg, e);
                }
            }
            StringBuilder resultMsg = new StringBuilder();
            if (failureNum > 0) {
                resultMsg.append(" ").append(failureNum).append(" ");
                resultMsg.append("<br/>").append(String.join("<br/>", failureMessages));
                throw new ServiceException(resultMsg.toString());
            } else {
                resultMsg.append(" ").append(successNum).append(" ");
            }
            return resultMsg.toString();
        }

    @Override
    public String getLogInfo(Long id) {
        String content = "";
        final String taskInstanceLogKey = AssetsDiscoveryLogBodyServiceImpl.DISCOVERY_TASK_LOG_KEY_PREFIX + id;

        if (redisService.hasKey(taskInstanceLogKey)) {
            content += redisService.get(taskInstanceLogKey) + "\n";
        } else {
            //获取表中的日志
            String logContent = IAssetsDiscoveryLogBodyService.getLog(id);
            if (logContent != null) {
                content += logContent + "\n";
            }
        }
        return content;
    }
}
