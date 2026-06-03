package com.datamaster.module.catalog.service.task.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.module.catalog.controller.admin.task.vo.CatalogTaskInstanceLogPageReqVO;
import com.datamaster.module.catalog.controller.admin.task.vo.CatalogTaskInstanceLogSaveReqVO;
import com.datamaster.module.catalog.dal.dataobject.task.CatalogTaskInstanceLogDO;
import com.datamaster.module.catalog.dal.mapper.task.CatalogTaskInstanceLogMapper;
import com.datamaster.module.catalog.service.task.ICatalogTaskInstanceLogService;
import com.datamaster.redis.service.IRedisService;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * 采集任务实例-日志Service业务层处理
 *
 * @author lili.shang
 * @date 2025-12-16
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class CatalogTaskInstanceLogServiceImpl extends ServiceImpl<CatalogTaskInstanceLogMapper, CatalogTaskInstanceLogDO> implements ICatalogTaskInstanceLogService {

    // 可放到常量类，这里内联
    public static final String Catalog_TASK_INSTANCE_LOG_KEY_PREFIX = "Catalog_TASK_INSTANCE_LOG:LOG:TASK:";
    public static final String FINALIZE_TOKEN = "FINALIZE_SESSION";

    @Resource
    private CatalogTaskInstanceLogMapper CatalogTaskInstanceLogMapper;

    @Resource
    @Lazy
    private IRedisService redisService;

    @Override
    public PageResult<CatalogTaskInstanceLogDO> getCatalogTaskInstanceLogPage(CatalogTaskInstanceLogPageReqVO pageReqVO) {
        return CatalogTaskInstanceLogMapper.selectPage(pageReqVO);
    }

    @Override
    public Long createCatalogTaskInstanceLog(CatalogTaskInstanceLogSaveReqVO createReqVO) {
        CatalogTaskInstanceLogDO dictType = BeanUtils.toBean(createReqVO, CatalogTaskInstanceLogDO.class);
        CatalogTaskInstanceLogMapper.insert(dictType);
        return 1L;
    }

    @Override
    public int updateCatalogTaskInstanceLog(CatalogTaskInstanceLogSaveReqVO updateReqVO) {
        // 相关校验

        // 更新采集任务实例-日志
        CatalogTaskInstanceLogDO updateObj = BeanUtils.toBean(updateReqVO, CatalogTaskInstanceLogDO.class);
        return CatalogTaskInstanceLogMapper.updateById(updateObj);
    }

    @Override
    public int removeCatalogTaskInstanceLog(Collection<Long> idList) {
        // 批量删除采集任务实例-日志
        return CatalogTaskInstanceLogMapper.deleteBatchIds(idList);
    }

    @Override
    public CatalogTaskInstanceLogDO getCatalogTaskInstanceLogById(Long id) {
        CatalogTaskInstanceLogDO catalogTaskInstanceLogDO = buildRealtimeLog(id);
        if(catalogTaskInstanceLogDO != null){
            return catalogTaskInstanceLogDO;
        }

        MPJLambdaWrapper<CatalogTaskInstanceLogDO> wrapper = new MPJLambdaWrapper<>();
        wrapper.eq(CatalogTaskInstanceLogDO::getTaskInstanceId, id)
                .eq(CatalogTaskInstanceLogDO::getDelFlag, "0")
                .orderByAsc(CatalogTaskInstanceLogDO::getCreateTime);
        List<CatalogTaskInstanceLogDO> CatalogTaskSchedulerDOS = CatalogTaskInstanceLogMapper.selectList(wrapper);
        CatalogTaskInstanceLogDO taskInstanceLogDO = CollectionUtils.isEmpty(CatalogTaskSchedulerDOS) ? null : CatalogTaskSchedulerDOS.get(0);
        taskInstanceLogDO.setStatus("2");
        return taskInstanceLogDO;
    }


    /**
     * 从 Redis 实时获取日志并封装为 CatalogTaskInstanceLogDO
     */
    private CatalogTaskInstanceLogDO buildRealtimeLog(Long taskInstanceId) {
        final String taskLogKey = Catalog_TASK_INSTANCE_LOG_KEY_PREFIX + taskInstanceId;
        String taskLog = redisService.get(taskLogKey);

        if (StringUtils.isBlank(taskLog)) {
            return null;
        }

        CatalogTaskInstanceLogDO entity = new CatalogTaskInstanceLogDO();
        entity.setTaskInstanceId(taskInstanceId);
        entity.setLogContent(taskLog);
        entity.setTime(new Date());
        entity.setValidFlag(Boolean.TRUE);
        entity.setDelFlag(Boolean.FALSE);

        String lastLine = getLastNotBlankLine(taskLog);
        if (StringUtils.contains(lastLine, FINALIZE_TOKEN)){
            entity.setStatus("2");
        }else {
            entity.setStatus("1");
        }
        return entity;
    }

    private String getLastNotBlankLine(String text) {
        if (StringUtils.isBlank(text)) {
            return null;
        }

        int end = text.length();

        while (end > 0) {
            int start = text.lastIndexOf("\n", end - 1);
            String line = text.substring(start + 1, end).trim();

            if (StringUtils.isNotBlank(line)) {
                return line;
            }

            end = start; // 继续往上找
        }

        return null;
    }


    @Override
    public List<CatalogTaskInstanceLogDO> getCatalogTaskInstanceLogList() {
        return CatalogTaskInstanceLogMapper.selectList();
    }


    /**
     * 采集任务实例日志写入（增量累积 + 会话结束一次性落库）
     *
     * @param taskInstanceId 任务实例 ID
     * @param taskId         任务 ID
     * @param logStr         本次追加日志
     */
    @Override
    public void taskInstanceLogAppend(Long taskInstanceId, Long taskId, String logStr) {
        if (taskInstanceId == null || StringUtils.isBlank(logStr)) {
            return;
        }

        final String taskLogKey = Catalog_TASK_INSTANCE_LOG_KEY_PREFIX + taskInstanceId;

        // 1. 读取已有日志
        String taskLog = redisService.get(taskLogKey);
        if (taskLog == null) {
            taskLog = "";
        }

        String time = DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss.SSS");
        logStr = time + " - " + logStr + "\n";

        // 2. 追加
        taskLog += logStr + (logStr.matches(".*\\r?\\n.*") ? "" : "\n");
        redisService.set(taskLogKey, taskLog);

        // 3. 会话结束标记，统一入库
        if (StringUtils.contains(logStr, FINALIZE_TOKEN)) {
            CatalogTaskInstanceLogDO entity = CatalogTaskInstanceLogDO.builder()
                    .taskInstanceId(taskInstanceId)
                    .taskId(taskId)
                    .time(new Date())
                    .logContent(taskLog)
                    .validFlag(Boolean.TRUE)
                    .delFlag(Boolean.FALSE)
                    .build();

            this.saveOrUpdateByPk(entity);

            // 4. 清 Redis，并短暂缓存（5 分钟）
            redisService.delete(taskLogKey);
            redisService.set(taskLogKey, taskLog, 60 * 5);
        }
    }

    @Override
    public int saveOrUpdateByPk(CatalogTaskInstanceLogDO entity) {
        CatalogTaskInstanceLogDO old = this.getOne(
                Wrappers.lambdaQuery(CatalogTaskInstanceLogDO.class)
                        .eq(CatalogTaskInstanceLogDO::getTaskInstanceId, entity.getTaskInstanceId())
        );

        if (old != null) {
            old.setLogContent(entity.getLogContent());
            old.setValidFlag(entity.getValidFlag());
            old.setDelFlag(entity.getDelFlag());
            old.setUpdateBy(entity.getUpdateBy());
            old.setUpdatorId(entity.getUpdatorId());
            old.setTime(entity.getTime());

            return CatalogTaskInstanceLogMapper.update(
                    old,
                    Wrappers.lambdaUpdate(CatalogTaskInstanceLogDO.class)
                            .eq(CatalogTaskInstanceLogDO::getTaskInstanceId, entity.getTaskInstanceId())
            );
        } else {
            return CatalogTaskInstanceLogMapper.insert(entity);
        }
    }
}
