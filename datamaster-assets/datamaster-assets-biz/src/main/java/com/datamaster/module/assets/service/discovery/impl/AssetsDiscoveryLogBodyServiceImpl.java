package com.datamaster.module.assets.service.discovery.impl;import cn.hutool.core.date.DateUtil;import com.baomidou.mybatisplus.core.toolkit.Wrappers;import com.baomidou.mybatisplus.extension.plugins.pagination.Page;import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;import lombok.extern.slf4j.Slf4j;import org.apache.commons.lang.StringUtils;import org.springframework.context.annotation.Lazy;import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;import com.datamaster.module.assets.dal.dataobject.discovery.AssetsDiscoveryLogBodyDO;import com.datamaster.module.assets.dal.mapper.discovery.AssetsDiscoveryLogBodyMapper;import com.datamaster.module.assets.service.discovery.IAssetsDiscoveryLogBodyService;import com.datamaster.redis.service.IRedisService;import javax.annotation.Resource;import java.util.Date;
/** * -Service * * @author DATAMASTER * @date 2025-10-15 */

@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)public class AssetsDiscoveryLogBodyServiceImpl extends ServiceImpl<AssetsDiscoveryLogBodyMapper, AssetsDiscoveryLogBodyDO> implements IAssetsDiscoveryLogBodyService {
// 可放到常量类，这里内联
    public static final String DISCOVERY_TASK_LOG_KEY_PREFIX = "DA_DISCOVERY:LOG:TASK:";
    public static final String FINALIZE_TOKEN = "FINALIZE_SESSION";    @Resource
    private AssetsDiscoveryLogBodyMapper AssetsDiscoveryLogBodyMapper;    @Resource
@Lazy    private IRedisService redisService;    @Override    public int saveOrUpdateByPk(AssetsDiscoveryLogBodyDO entity) {        AssetsDiscoveryLogBodyDO old = this.getOne(Wrappers.lambdaQuery(AssetsDiscoveryLogBodyDO.class)                .eq(AssetsDiscoveryLogBodyDO::getTaskId, entity.getTaskId()));        if (old != null) {            old.setLogContent(entity.getLogContent());            old.setValidFlag(entity.getValidFlag());            old.setDelFlag(entity.getDelFlag());            old.setUpdateBy(entity.getUpdateBy());            old.setUpdaterId(entity.getUpdaterId());            old.setRemark(entity.getRemark());            return AssetsDiscoveryLogBodyMapper.update(old, Wrappers.lambdaUpdate(AssetsDiscoveryLogBodyDO.class)                    .eq(AssetsDiscoveryLogBodyDO::getTaskId, entity.getTaskId()));        } else {            return AssetsDiscoveryLogBodyMapper.insert(entity);        }    }
@Override    public String getLatestLog(Long taskId) {        Page<AssetsDiscoveryLogBodyDO> page = this.page(new Page<>(1, 1),                Wrappers.lambdaQuery(AssetsDiscoveryLogBodyDO.class)                        .eq(AssetsDiscoveryLogBodyDO::getTaskId, taskId)                        .orderByDesc(AssetsDiscoveryLogBodyDO::getTm));        if (page.getRecords().isEmpty()) {            return null;        }        return page.getRecords().get(0).getLogContent();    }
@Override    public String getLog(Long taskId) {        AssetsDiscoveryLogBodyDO row = this.getOne(Wrappers.lambdaQuery(AssetsDiscoveryLogBodyDO.class)                .eq(AssetsDiscoveryLogBodyDO::getTaskId, taskId));        return row == null ? null : row.getLogContent();
    }
    /***     *  +      * 1)  Redis     * 2)  FINALIZE_SESSION DA_DISCOVERY_LOG_BODY     * 3)  5      *     * @param taskId   ID     * @param logStr       */

@Override    public void taskLogAppend(Long taskId, String logStr) {
// 1. 基本校验
    if (taskId == null || StringUtils.isBlank(logStr)) {
    return;
    }
    final String taskLogKey = DISCOVERY_TASK_LOG_KEY_PREFIX + taskId;

// 2. 读取 Redis 既有日志（没有则置空串）
    String taskLog = redisService.get(taskLogKey);
    if (taskLog == null) {
    taskLog = "";
    }
    String time = DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss.SSS");
    logStr = time + " - " + logStr+ "\n";

// 3. 追加本次增量（若没有换行，以换行收尾）
    taskLog += logStr + (logStr.matches(".*\\r?\\n.*") ? "" : "\n");
    redisService.set(taskLogKey, taskLog);

// 4. 如检测到会话结束标记，则一次性入库，并做 5 分钟缓存
    if (StringUtils.contains(logStr, FINALIZE_TOKEN)) {
// 入库：复合主键 (taskId, tm)；tm 取当前时间
    AssetsDiscoveryLogBodyDO entity = AssetsDiscoveryLogBodyDO.builder()                    .taskId(taskId)                    .tm(new Date())                    .logContent(taskLog)                    .validFlag(Boolean.TRUE)                    .delFlag(Boolean.FALSE)                    .build();

// 复用你之前实现的复合主键保存/更新语义
    this.saveOrUpdateByPk(entity);

// 重置并短期缓存，便于前端“会话结束后”仍可查询
    redisService.delete(taskLogKey);
    redisService.set(taskLogKey, taskLog, 60 * 5);
    }
    }
@Override    public boolean deleteByPk(Long taskId) {        if (taskId == null) {            return false;        }        return this.remove(                Wrappers.lambdaQuery(AssetsDiscoveryLogBodyDO.class)                        .eq(AssetsDiscoveryLogBodyDO::getTaskId, taskId)        );
    }}