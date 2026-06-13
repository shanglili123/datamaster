package com.datamaster.module.catalog.service.task.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.datamaster.common.constant.HttpStatus;
import com.datamaster.common.core.domain.BatchDeleteCheck;
import com.datamaster.common.core.domain.entity.SysUser;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.database.DataSourceFactory;
import com.datamaster.common.database.DbQuery;
import com.datamaster.common.database.constants.DbQueryProperty;
import com.datamaster.common.database.constants.DbType;
import com.datamaster.common.database.core.DbColumn;
import com.datamaster.common.database.core.DbName;
import com.datamaster.common.database.core.DbTable;
import com.datamaster.common.database.exception.DataQueryException;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.module.taxonomy.api.project.ITaxonomyProjectApi;
import com.datamaster.module.taxonomy.api.sourceSystem.dto.TaxonomySourceSystemRespDTO;
import com.datamaster.module.taxonomy.api.sourceSystem.service.ITaxonomySourceSystemApiService;
import com.datamaster.module.assets.api.datasource.dto.AssetsDatasourceRespDTO;
import com.datamaster.module.assets.api.service.asset.IAssetsDatasourceApiService;
import com.datamaster.module.catalog.api.service.task.ICatalogTaskApiService;
import com.datamaster.module.catalog.controller.admin.metadata.vo.*;
import com.datamaster.module.catalog.controller.admin.task.vo.*;
import com.datamaster.module.catalog.dal.dataobject.metadata.CatalogColumnDO;
import com.datamaster.module.catalog.dal.dataobject.metadata.CatalogDbDO;
import com.datamaster.module.catalog.dal.dataobject.metadata.CatalogTableDO;
import com.datamaster.module.catalog.dal.dataobject.task.CatalogTaskDO;
import com.datamaster.module.catalog.dal.dataobject.task.CatalogTaskInstanceDO;
import com.datamaster.module.catalog.dal.dataobject.task.CatalogTaskSchedulerDO;
import com.datamaster.module.catalog.dal.dataobject.task.CatalogTaskScopeDO;
import com.datamaster.module.catalog.dal.mapper.metadata.CatalogColumnMapper;
import com.datamaster.module.catalog.dal.mapper.metadata.CatalogDbMapper;
import com.datamaster.module.catalog.dal.mapper.metadata.CatalogTableMapper;
import com.datamaster.module.catalog.dal.mapper.task.CatalogTaskMapper;
import com.datamaster.module.catalog.enums.CollectionScopeEnum;
import com.datamaster.module.catalog.enums.SchedulerStatusEnum;
import com.datamaster.module.catalog.service.columnLog.ICatalogColumnLogService;
import com.datamaster.module.catalog.service.metadata.ICatalogColumnService;
import com.datamaster.module.catalog.service.metadata.ICatalogDbService;
import com.datamaster.module.catalog.service.metadata.ICatalogTableService;
import com.datamaster.module.catalog.service.metadata.dialect.DatabaseDialect;
import com.datamaster.module.catalog.service.metadata.dialect.DatabaseDialectFactory;
import com.datamaster.module.catalog.service.scheduler.CatalogTaskDolphinSchedulerService;
import com.datamaster.module.catalog.service.tableColumnRelLog.ICatalogTableColumnRelLogService;
import com.datamaster.module.catalog.service.tableLog.ICatalogTableLogService;
import com.datamaster.module.catalog.service.task.*;
import com.datamaster.module.system.service.ISysUserService;
import com.datamaster.redis.service.IRedisService;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 采集任务Service业务层处理
 *
 * @author DATAMASTER
 * @date 2025-12-16
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class CatalogTaskServiceImpl extends ServiceImpl<CatalogTaskMapper, CatalogTaskDO> implements ICatalogTaskService, ICatalogTaskApiService {
    @Resource
    private CatalogTaskMapper CatalogTaskMapper;

    @Resource
    private ICatalogTaskSchedulerService CatalogTaskSchedulerService;
    @Resource
    private ICatalogTaskScopeService CatalogTaskScopeService;
    @Resource
    private IAssetsDatasourceApiService daDatasourceApiService;
    @Resource
    private ICatalogTaskInstanceService CatalogTaskInstanceService;
    @Resource
    private ICatalogTaskInstanceLogService CatalogTaskInstanceLogService;

    @Autowired
    @Lazy
    private DataSourceFactory dataSourceFactory;

    @Resource
    @Lazy
    private IRedisService redisService;

    //外部api
    @Resource
    @Lazy
    private ICatalogDbService CatalogDbService;
    @Resource
    private ICatalogTableService CatalogTableService;
    @Resource
    private ICatalogColumnService CatalogColumnService;

    @Resource
    private CatalogDbTxService CatalogDbTxService;
    @Resource
    private CatalogTableTxService CatalogTableTxService;
    @Resource
    private CatalogTaskDolphinSchedulerService CatalogTaskDolphinSchedulerService;


    @Resource
    private ICatalogTableLogService CatalogTableLogService;
    @Resource
    private ICatalogColumnLogService CatalogColumnLogService;
    @Resource
    private ICatalogTableColumnRelLogService CatalogTableColumnRelLogService;
    @Resource
    private CatalogDbMapper CatalogDbMapper;
    @Resource
    private CatalogTableMapper CatalogTableMapper;
    @Resource
    private CatalogColumnMapper CatalogColumnMapper;
    @Resource
    private ISysUserService sysUserService;
    @Resource
    private ITaxonomySourceSystemApiService attSourceSystemApiService;
    @Resource
    private ITaxonomyProjectApi taxonomyProjectApi;


    @Override
    public boolean existsBySourceSystemName(String sourceSystemName) {
        return CatalogTaskMapper.existsBySourceSystemName(sourceSystemName);
    }


    @Override
    public PageResult<CatalogTaskDO> getCatalogTaskPage(CatalogTaskPageReqVO pageReqVO) {
        PageResult<CatalogTaskDO> CatalogTaskDOPageResult = CatalogTaskMapper.selectPage(pageReqVO);

        List<CatalogTaskDO> rows = CatalogTaskDOPageResult.getRows();

        if (CollectionUtils.isEmpty(rows)) {
            return CatalogTaskDOPageResult;
        }

        // FIXME(用户查询避免循环查询，临时方案)  使用 Map 缓存用户信息,避免重复查询
        Map<Long, SysUser> userCache = Maps.newHashMap();
        for (CatalogTaskDO row : rows) {
            // 获取创建人手机号
            Long creatorId = row.getCreatorId();
            if (creatorId != null && !userCache.containsKey(creatorId)) {
                SysUser sysUser = sysUserService.selectUserById(creatorId);
                if (sysUser != null) {
                    userCache.put(creatorId, sysUser);
                }
            }
            SysUser creatorUser = userCache.get(creatorId);
            if (creatorUser != null) {
                row.setCreatePhoneNumber(creatorUser.getPhonenumber());
            }

            CatalogTaskInstanceDO CatalogTaskInstanceByTaskId = CatalogTaskInstanceService.getCatalogTaskInstanceByTaskId(row.getId());
            if (CatalogTaskInstanceByTaskId != null) {
                row.setLastExecuteTime(DateUtil.format(CatalogTaskInstanceByTaskId.getCreateTime(), "yyyy-MM-dd HH:mm:ss"));
            }

        }
        CatalogTaskDOPageResult.setRows(rows);

        return CatalogTaskDOPageResult;
    }

    @Override
    public Long createCatalogTask(CatalogTaskSaveReqVO createReqVO) {
        daDatasourceApiService.getDatabaseListByDatasourceId(createReqVO.getDatasourceId());
        // 校验任务是否重复
        validateDuplicateTask(createReqVO, null);

        CatalogTaskDO dictType = BeanUtils.toBean(createReqVO, CatalogTaskDO.class);
        fillProjectRelation(dictType);
        if (StringUtils.isEmpty(dictType.getStatus())) {
            dictType.setStatus(SchedulerStatusEnum.DISABLED.getValue());
        }
        CatalogTaskMapper.insert(dictType);
        Long id = dictType.getId();

        // 创建 DolphinScheduler 任务定义
        String projectCode = resolveProjectCode(dictType);
        String taskCode = CatalogTaskDolphinSchedulerService.createTaskDefinition(projectCode, dictType.getName(), id);

        // 先上线任务（DolphinScheduler要求：只有上线的任务才能创建调度器）
        CatalogTaskDolphinSchedulerService.onlineTask(projectCode, taskCode);

        // 创建调度器
        Long schedulerId = CatalogTaskDolphinSchedulerService.createScheduler(projectCode, taskCode, dictType.getCronExpression());

        //存储调度信息
        CatalogTaskSchedulerSaveReqVO schedulerSaveReqVO = new CatalogTaskSchedulerSaveReqVO(dictType);
        schedulerSaveReqVO.setJobId(String.valueOf(schedulerId));
        schedulerSaveReqVO.setTaskCode(taskCode);  // 设置任务编码到调度表
        schedulerSaveReqVO.setStatus(SchedulerStatusEnum.DISABLED.getValue());
        CatalogTaskSchedulerService.createCatalogTaskScheduler(schedulerSaveReqVO);

        if (StringUtils.equals("1", createReqVO.getCollectionScope())) {
            List<CatalogTaskScopeSaveReqVO> scopeSaveReqVOS = createReqVO.getScopeSaveReqVOS();
            for (CatalogTaskScopeSaveReqVO scopeSaveReqVO : scopeSaveReqVOS) {
                scopeSaveReqVO.setTaskId(id);
                scopeSaveReqVO.setProjectId(dictType.getProjectId());
                scopeSaveReqVO.setProjectCode(dictType.getProjectCode());
                CatalogTaskScopeService.createCatalogTaskScope(scopeSaveReqVO);
            }
        }

        return dictType.getId();
    }

    @Override
    public int updateCatalogTask(CatalogTaskSaveReqVO updateReqVO) {
        // 校验任务是否重复(排除当前任务自身)
        validateDuplicateTask(updateReqVO, updateReqVO.getId());

        // 1. 更新采集任务
        CatalogTaskDO updateObj = BeanUtils.toBean(updateReqVO, CatalogTaskDO.class);
        fillProjectRelation(updateObj);
        int rows = CatalogTaskMapper.updateById(updateObj);

        // 2. 查询调度信息
        CatalogTaskSchedulerDO scheduler = CatalogTaskSchedulerService.getCatalogTaskSchedulerBytaskId(updateObj.getId());
        if (scheduler != null) {
            // 3. 只在有变更时才更新
            CatalogTaskSchedulerSaveReqVO schedulerSaveReqVO = new CatalogTaskSchedulerSaveReqVO();
            schedulerSaveReqVO.setId(scheduler.getId());

            boolean needUpdate = false;

            // cron 表达式
            String cronExpression = updateReqVO.getCronExpression();
            if (StringUtils.isNotEmpty(cronExpression) && !StringUtils.equals(cronExpression, scheduler.getCronExpression())) {
                schedulerSaveReqVO.setCronExpression(cronExpression);

                // 获取任务编码（从调度表）
                String taskCode = scheduler.getTaskCode();

                if (StringUtils.isNotEmpty(taskCode)) {
                    // 更新 DolphinScheduler 调度器
                    String projectCode = resolveProjectCode(updateObj, scheduler);
                    Long newSchedulerId = CatalogTaskDolphinSchedulerService.updateScheduler(
                            projectCode, Long.parseLong(scheduler.getJobId()), taskCode, cronExpression);
                    schedulerSaveReqVO.setJobId(String.valueOf(newSchedulerId));
                    schedulerSaveReqVO.setProjectId(updateObj.getProjectId());
                    schedulerSaveReqVO.setProjectCode(projectCode);
                }
                needUpdate = true;
            }

            // 调度状态
            String schedulerStatus = updateReqVO.getSchedulerStatus();
            if (StringUtils.isNotEmpty(schedulerStatus) && !StringUtils.equals(schedulerStatus, scheduler.getStatus())) {
                schedulerSaveReqVO.setStatus(schedulerStatus);
                needUpdate = true;
            }

            // 4. 有变化才落库
            if (needUpdate) {
                CatalogTaskSchedulerService.updateCatalogTaskScheduler(schedulerSaveReqVO);
            }

        }

        if (StringUtils.equals("1", updateReqVO.getCollectionScope())) {
            //删除
            CatalogTaskScopeService.removeCatalogTaskScopeBytaskId(updateObj.getId());
            //新增
            List<CatalogTaskScopeSaveReqVO> scopeSaveReqVOS = updateReqVO.getScopeSaveReqVOS();
            for (CatalogTaskScopeSaveReqVO scopeSaveReqVO : scopeSaveReqVOS) {
                scopeSaveReqVO.setId(null);
                scopeSaveReqVO.setTaskId(updateObj.getId());
                scopeSaveReqVO.setProjectId(updateObj.getProjectId());
                scopeSaveReqVO.setProjectCode(updateObj.getProjectCode());
                CatalogTaskScopeService.createCatalogTaskScope(scopeSaveReqVO);
            }
        }
        return rows;
    }

    @Override
    public int removeCatalogTask(Collection<Long> idList) {
        for (Long id : idList) {
            CatalogTaskSchedulerDO scheduler = CatalogTaskSchedulerService.getCatalogTaskSchedulerBytaskId(id);
            CatalogTaskDO task = CatalogTaskMapper.selectById(id);

            // 先下线任务和调度器
            if (task != null && scheduler != null && StringUtils.isNotEmpty(scheduler.getTaskCode())) {
                try {
                    Long schedulerId = StringUtils.isNotEmpty(scheduler.getJobId()) ?
                            Long.parseLong(scheduler.getJobId()) : null;
                    String projectCode = resolveProjectCode(task, scheduler);
                    CatalogTaskDolphinSchedulerService.offlineTaskAndScheduler(projectCode, scheduler.getTaskCode(), schedulerId);
                } catch (Exception e) {
                    log.warn("下线任务失败，taskId={}", id, e);
                }

                // 删除任务
                try {
                    String projectCode = resolveProjectCode(task, scheduler);
                    CatalogTaskDolphinSchedulerService.deleteTask(projectCode, scheduler.getTaskCode());
                } catch (Exception e) {
                    log.warn("删除DolphinScheduler任务失败，taskId={}", id, e);
                }
            }
        }
        // 批量删除采集任务
        return CatalogTaskMapper.deleteBatchIds(idList);
    }

    @Override
    public BatchDeleteCheck<Long> batchDeleteCheck(List<Long> list) {
        int cannotDeleteCount = 0;
        List<Long> canDeleteIds = new ArrayList<>();
        for (Long id : list) {
            CatalogTaskRespVO CatalogTaskByIdNew = this.getCatalogTaskByIdNew(id);
            if (StringUtils.equals("1", CatalogTaskByIdNew.getStatus())
                    || StringUtils.equals("1", CatalogTaskByIdNew.getSchedulerStatus())) {
                cannotDeleteCount++;
                continue;
            }
            canDeleteIds.add(id);
        }
        return new BatchDeleteCheck<>(cannotDeleteCount, canDeleteIds);
    }


    @Override
    public CatalogTaskDO getCatalogTaskById(Long id) {
        MPJLambdaWrapper<CatalogTaskDO> lambdaWrapper = new MPJLambdaWrapper();

        lambdaWrapper.selectAll(CatalogTaskDO.class)
                .select("t5.NICK_NAME AS personChargeName")
                .leftJoin("SYSTEM_USER t5 ON t.LEADER = t5.USER_ID AND t5.DEL_FLAG = '0'")
                .eq(CatalogTaskDO::getId, id);

        return CatalogTaskMapper.selectOne(lambdaWrapper);
    }

    @Override
    public CatalogTaskRespVO getCatalogTaskByIdNew(Long id) {
        CatalogTaskRespVO bean = BeanUtils.toBean(this.getCatalogTaskById(id), CatalogTaskRespVO.class);

        CatalogTaskSchedulerDO scheduler = CatalogTaskSchedulerService.getCatalogTaskSchedulerBytaskId(id);
        if (scheduler != null) {
            bean.setCronExpression(scheduler.getCronExpression());
            bean.setSchedulerStatus(scheduler.getStatus());
            bean.setJobId(scheduler.getJobId());
            bean.setTaskCode(scheduler.getTaskCode());  // 从调度表获取 taskCode
        }

        List<CatalogTaskScopeDO> CatalogTaskScopeDOS = CatalogTaskScopeService.getCatalogTaskScopeListBytaskId(id);
        bean.setScopeSaveReqVOS(CatalogTaskScopeDOS);

        AssetsDatasourceRespDTO CatalogDatasourceById = daDatasourceApiService.getDatasourceById(bean.getDatasourceId());
        bean.setDatasourceDO(CatalogDatasourceById);

        CatalogTaskInstanceDO CatalogTaskInstanceByTaskId = CatalogTaskInstanceService.getCatalogTaskInstanceByTaskId(id);
        if (CatalogTaskInstanceByTaskId != null) {
            bean.setLastExecuteTime(DateUtil.format(CatalogTaskInstanceByTaskId.getCreateTime(), "yyyy-MM-dd HH:mm:ss"));
        }

        return bean;
    }

    @Override
    public List<CatalogTaskDO> getCatalogTaskList() {
        return CatalogTaskMapper.selectList();
    }

    @Override
    public Map<Long, CatalogTaskDO> getCatalogTaskMap() {
        List<CatalogTaskDO> CatalogTaskList = CatalogTaskMapper.selectList();
        return CatalogTaskList.stream().collect(Collectors.toMap(CatalogTaskDO::getId, CatalogTaskDO -> CatalogTaskDO,
                // 保留已存在的值
                (existing, replacement) -> existing));
    }


    /**
     * 导入采集任务数据
     *
     * @param importExcelList 采集任务数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName        操作用户
     * @return 结果
     */
    @Override
    public String importCatalogTask(List<CatalogTaskRespVO> importExcelList, boolean isUpdateSupport, String operName) {
        if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
            throw new ServiceException("导入数据不能为空！");
        }

        int successNum = 0;
        int failureNum = 0;
        List<String> successMessages = new ArrayList<>();
        List<String> failureMessages = new ArrayList<>();

        for (CatalogTaskRespVO respVO : importExcelList) {
            try {
                CatalogTaskDO CatalogTaskDO = BeanUtils.toBean(respVO, CatalogTaskDO.class);
                Long CatalogTaskId = respVO.getId();
                if (isUpdateSupport) {
                    if (CatalogTaskId != null) {
                        CatalogTaskDO existingCatalogTask = CatalogTaskMapper.selectById(CatalogTaskId);
                        if (existingCatalogTask != null) {
                            CatalogTaskMapper.updateById(CatalogTaskDO);
                            successNum++;
                            successMessages.add("数据更新成功，ID为 " + CatalogTaskId + " 的采集任务记录。");
                        } else {
                            failureNum++;
                            failureMessages.add("数据更新失败，ID为 " + CatalogTaskId + " 的采集任务记录不存在。");
                        }
                    } else {
                        failureNum++;
                        failureMessages.add("数据更新失败，某条记录的ID不存在。");
                    }
                } else {
                    QueryWrapper<CatalogTaskDO> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("id", CatalogTaskId);
                    CatalogTaskDO existingCatalogTask = CatalogTaskMapper.selectOne(queryWrapper);
                    if (existingCatalogTask == null) {
                        CatalogTaskMapper.insert(CatalogTaskDO);
                        successNum++;
                        successMessages.add("数据插入成功，ID为 " + CatalogTaskId + " 的采集任务记录。");
                    } else {
                        failureNum++;
                        failureMessages.add("数据插入失败，ID为 " + CatalogTaskId + " 的采集任务记录已存在。");
                    }
                }
            } catch (Exception e) {
                failureNum++;
                String errorMsg = "数据导入失败，错误信息：" + e.getMessage();
                failureMessages.add(errorMsg);
                log.error(errorMsg, e);
            }
        }
        StringBuilder resultMsg = new StringBuilder();
        if (failureNum > 0) {
            resultMsg.append("很抱歉，导入失败！共 ").append(failureNum).append(" 条数据格式不正确，错误如下：");
            resultMsg.append("<br/>").append(String.join("<br/>", failureMessages));
            throw new ServiceException(resultMsg.toString());
        } else {
            resultMsg.append("恭喜您，数据已全部导入成功！共 ").append(successNum).append(" 条。");
        }
        return resultMsg.toString();
    }

    @Override
    public List<CatalogTaskScopeDO> getRealtimeCatalogTaskScopeList(Long id) {
        List<DbName> dbNameList = daDatasourceApiService.getDatabaseListByDatasourceId(id);
        List<CatalogTaskScopeDO> taskScopeDOList = new ArrayList<>();

        if (CollectionUtils.isEmpty(dbNameList)) {
            return taskScopeDOList;
        }

        for (DbName dbName : dbNameList) {
            List<DbName> children = dbName.getChildren();
            if (CollectionUtils.isNotEmpty(children)) {
                for (DbName child : children) {
                    CatalogTaskScopeDO CatalogTaskScopeDO = new CatalogTaskScopeDO();
                    CatalogTaskScopeDO.setDbName(dbName.getDbName());
                    CatalogTaskScopeDO.setSchemaName(child.getDbName());
                    taskScopeDOList.add(CatalogTaskScopeDO);
                }
            } else {
                CatalogTaskScopeDO CatalogTaskScopeDO = new CatalogTaskScopeDO();
                CatalogTaskScopeDO.setDbName(dbName.getDbName());
                taskScopeDOList.add(CatalogTaskScopeDO);
            }
        }
        return taskScopeDOList;
    }

    @Override
    public Map<String, Object> updateReleaseJobTask(CatalogTaskSaveReqVO CatalogTask) {
        CatalogTaskRespVO CatalogTaskByIdNew = this.getCatalogTaskByIdNew(CatalogTask.getId());
        String status = CatalogTaskByIdNew.getStatus();
        if (StringUtils.equals(status, CatalogTask.getStatus())) {
            return new HashMap<>();
        }

        if (StringUtils.equals("0", CatalogTask.getStatus()) && StringUtils.equals("1", CatalogTask.getSchedulerStatus())) {
            throw new ServiceException("请先将调度下线！");
        }

        CatalogTaskDO updateObj = new CatalogTaskDO();
        updateObj.setId(CatalogTask.getId());
        updateObj.setStatus(CatalogTask.getStatus());

        int rows = CatalogTaskMapper.updateById(updateObj);

        Map<String, Object> result = new HashMap<>();
        result.put("rows", rows);
        return result;
    }

    @Override
    public Map<String, Object> updateReleaseSchedule(CatalogTaskSaveReqVO CatalogTask) {
        CatalogTaskRespVO CatalogTaskByIdNew = this.getCatalogTaskByIdNew(CatalogTask.getId());
        String schedulerStatus = CatalogTaskByIdNew.getSchedulerStatus();
        if (StringUtils.equals(schedulerStatus, CatalogTask.getStatus())) {
            return new HashMap<>();
        }

        // 获取调度器信息
        CatalogTaskSchedulerDO scheduler = CatalogTaskSchedulerService.getCatalogTaskSchedulerBytaskId(CatalogTask.getId());

        if (scheduler != null && StringUtils.isNotEmpty(scheduler.getTaskCode())) {
            Long schedulerId = StringUtils.isNotEmpty(scheduler.getJobId()) ?
                    Long.parseLong(scheduler.getJobId()) : null;

            // 下线调度器（禁用定时触发）
            if (SchedulerStatusEnum.isDisabled(CatalogTask.getStatus())) {
                CatalogTaskDolphinSchedulerService.offlineSchedulerOnly(resolveProjectCode(CatalogTaskByIdNew, scheduler), schedulerId);
            }

            // 上线调度器（启用定时触发）
            if (SchedulerStatusEnum.isEnabled(CatalogTask.getStatus())) {
                CatalogTaskDolphinSchedulerService.onlineSchedulerOnly(resolveProjectCode(CatalogTaskByIdNew, scheduler), schedulerId);
            }
        }

        CatalogTaskSchedulerSaveReqVO updateReqVO = new CatalogTaskSchedulerSaveReqVO();
        updateReqVO.setTaskId(CatalogTask.getId());
        updateReqVO.setStatus(CatalogTask.getStatus());
        CatalogTaskSchedulerService.updateReleaseSchedule(updateReqVO);

        return new HashMap<>();
    }

    @Override
    public Map<String, Object> runJobOnce(CatalogTaskSaveReqVO CatalogTask) {
        String redisKey = buildRunLockKey(CatalogTask.getId());
        if (!checkTaskRunLock(redisKey)) {
            throw new RuntimeException("历史任务未执行完毕，请稍后重试");
        }
        // FIXME 记录用redis解决执行一次的人
        redisService.set(redisKey + ":creatorId", CatalogTask.getCreatorId().toString(), 60 * 60 * 12);
        redisService.set(redisKey + ":createBy", CatalogTask.getCreateBy().toString(), 60 * 60 * 12);

        CatalogTaskRespVO CatalogTaskByIdNew = this.getCatalogTaskByIdNew(CatalogTask.getId());

        // 使用 DolphinScheduler 立即执行任务
        if (CatalogTaskByIdNew != null) {
            // 从调度信息中获取 taskCode
            CatalogTaskSchedulerDO scheduler = CatalogTaskSchedulerService.getCatalogTaskSchedulerBytaskId(CatalogTask.getId());
            if (scheduler != null && StringUtils.isNotEmpty(scheduler.getTaskCode())) {
                CatalogTaskDolphinSchedulerService.startTask(resolveProjectCode(CatalogTaskByIdNew, scheduler), scheduler.getTaskCode());
            }
        }

        return new HashMap<>();
    }

    @Override
    public boolean runDaDiscoveryTask(Long taskId) {

        String redisKey = buildRunLockKey(taskId);
        if (!acquireRunLock(redisKey)) {
            throw new RuntimeException("历史任务未执行完毕，请稍后重试");
        }

        CatalogTaskRespVO task = loadTask(taskId);

        CatalogTaskInstanceDO instance = createTaskInstance(task);
        Long instanceId = instance.getId();

        // ⚠️ 从这里开始：必须保证日志
        safeLog(instanceId, taskId, "任务开始执行");

        try {
            TableProcessResult tableProcessResult = executeTaskSafely(task, instance);

            if (tableProcessResult != null) {
                Long addCount = tableProcessResult.getAddCount();
                Long delCount = tableProcessResult.getDelCount();
                Long updateCount = tableProcessResult.getUpdateCount();
                instance.setAddCount(addCount);
                instance.setDelCount(delCount);
                instance.setUpdateCount(updateCount);
                instance.setTotalCount(tableProcessResult.getTotalCount());
                instance.setSuccessCount(tableProcessResult.getSuccessCount());
                instance.setFailCount(tableProcessResult.getTotalCount() - tableProcessResult.getSuccessCount());

                if (StringUtils.equals("2", task.getCollectType())) {
                    if ((addCount != null && addCount != 0L)
                            || (delCount != null && delCount != 0L)
                            || (updateCount != null && updateCount != 0L)) {
                        instance.setValidFlag(Boolean.TRUE);
                    }
                }
            }
            markSuccess(instance);
            safeLog(instanceId, taskId, "任务执行成功");
            return true;
        } catch (Exception e) {
            redisService.delete(redisKey);
            markFail(instance, e);
            safeLog(instanceId, taskId, "任务执行失败：" + e.getMessage());
            return false;
        } finally {
            safeLog(instanceId, taskId, String.format("任务执行完成汇总：表总数=%d，成功表=%d，失败表=%d，耗时=%d秒", instance.getTotalCount(), instance.getSuccessCount(), instance.getFailCount(), instance.getDuration()));
            finalizeTask(redisKey, instance);
        }
    }

    private String buildRunLockKey(Long taskId) {
        // 统一前缀，避免与其它模块 key 冲突
        return "mc:task:run:" + taskId;
    }

    private CatalogTaskRespVO loadTask(Long taskId) {
        CatalogTaskRespVO task = this.getCatalogTaskByIdNew(taskId);
        if (task == null) {
            throw new DataQueryException("采集任务不存在，taskId=" + taskId);
        }
        return task;
    }

    private boolean checkTaskRunLock(String redisKey) {
        String status = redisService.get(redisKey);
        if (StringUtils.isNotBlank(status) && "1".equals(status)) {
            return false;
        }
        return true;
    }

    private boolean acquireRunLock(String redisKey) {
        String status = redisService.get(redisKey);
        if (StringUtils.isNotBlank(status) && "1".equals(status)) {
            return false;
        }
        redisService.set(redisKey, "1", 60 * 60 * 12);
        return true;
    }

    private CatalogTaskInstanceDO createTaskInstance(CatalogTaskRespVO task) {
        CatalogTaskInstanceDO instance = CatalogTaskInstanceDO.builder()
                .taskId(task.getId())
                .sourceSystemId(task.getSourceSystemId())
                .sourceSystemName(task.getSourceSystemName())
                .collectionMode(task.getCollectionMode())
                .collectionScope(task.getCollectionScope())
                .status("1")
                .successCount(0L)
                .failCount(0L)
                .totalCount(0L)
                .addCount(0L)
                .delCount(0L)
                .updateCount(0L)
                .startTime(new Date())
                .validFlag(Boolean.TRUE)
                .delFlag(Boolean.FALSE)
                .build();
        String creatorId = redisService.get(buildRunLockKey(task.getId()) + ":creatorId");
        String createBy = redisService.get(buildRunLockKey(task.getId()) + ":createBy");
        instance.setCreateBy(StringUtils.isNotEmpty(createBy) ? createBy : "System Collection Task");
        instance.setCreatorId(StringUtils.isNotEmpty(creatorId) ? Long.parseLong(creatorId) : 1L);

        String collectType = task.getCollectType();
        if (StringUtils.equals("2", collectType)) {
            instance.setValidFlag(Boolean.FALSE);
        }

        Long CatalogTaskInstance = CatalogTableTxService.runInNewTx(() -> CatalogTaskInstanceService.createCatalogTaskInstance(instance));
//        Long CatalogTaskInstance = CatalogTaskInstanceService.createCatalogTaskInstance(instance);
        instance.setId(CatalogTaskInstance);
        return instance;
    }

    private void safeLog(Long instanceId, Long taskId, String msg) {
        try {
            CatalogTaskInstanceLogService.taskInstanceLogAppend(instanceId, taskId, msg);
        } catch (Exception e) {
            // ⚠️ 日志失败只允许打本地日志，绝不再抛，避免覆盖业务异常
            log.error("任务实例日志写入失败 instanceId={}, msg={}", instanceId, msg, e);
        }
    }


    private void markSuccess(CatalogTaskInstanceDO instance) {
        instance.setStatus("9");
        instance.setEndTime(new Date());
        instance.setDuration((instance.getEndTime().getTime() - instance.getStartTime().getTime()) / 1000);
        CatalogTaskInstanceService.updateById(instance);
    }


    private void markFail(CatalogTaskInstanceDO instance, Exception e) {
        instance.setStatus("2");
        instance.setFailCause(e.getMessage());
        instance.setEndTime(new Date());
        instance.setDuration((instance.getEndTime().getTime() - instance.getStartTime().getTime()) / 1000);
        CatalogTaskInstanceService.updateById(instance);
    }

    private void finalizeTask(String redisKey, CatalogTaskInstanceDO instance) {
        safeLog(instance.getId(), instance.getTaskId(), "FINALIZE_SESSION");
        safeLog(instance.getId(), instance.getTaskId(), "任务结束");
        redisService.set(redisKey, "2", 300);
    }

    private AssetsDatasourceRespDTO prepareDatasource(CatalogTaskRespVO task, CatalogTaskInstanceDO instance) {

        Long taskId = task.getId();
        Long instanceId = instance.getId();

        safeLog(instanceId, taskId, "任务执行-开始获取数据源信息");

        Long datasourceId = task.getDatasourceId();
        if (datasourceId == null) {
            safeLog(instanceId, taskId, "任务执行-数据源ID为空，无法继续");
            throw new DataQueryException("数据源ID为空");
        }

        AssetsDatasourceRespDTO datasource;
        try {
            datasource = daDatasourceApiService.getDatasourceById(datasourceId);
        } catch (Exception e) {
            safeLog(instanceId, taskId, "任务执行-获取数据源信息异常：" + e.getMessage());
            throw e;
        }

        if (datasource == null) {
            safeLog(instanceId, taskId, "任务执行-数据源详情信息查询失败，datasourceId=" + datasourceId);
            throw new DataQueryException("数据源详情信息查询失败");
        }

        safeLog(instanceId, taskId, "任务执行-获取数据源信息成功，datasourceId=" + datasourceId);
        return datasource;
    }

    public static boolean isInBlacklist(String input, String blacklist) {
        if (StringUtils.isBlank(blacklist) || StringUtils.isBlank(input)) {
            return false;
        }

        String[] list = blacklist.split(",");

        for (String item : list) {
            String black = item.trim();
            if (input.equalsIgnoreCase(black)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 主流程
     *
     * @param task
     * @param instance
     */
    private TableProcessResult executeTaskSafely(CatalogTaskRespVO task, CatalogTaskInstanceDO instance) {

        safeLog(instance.getId(), task.getId(), "开始准备数据源");

        AssetsDatasourceRespDTO datasource = prepareDatasource(task, instance);

        safeLog(instance.getId(), task.getId(), "开始加载数据源连接");


        Long instanceId = instance.getId();
        Long taskId = task.getId();

        String scopeDesc;
        if ("1".equals(task.getCollectionScope())) {
            scopeDesc = "自定义库";
        } else if ("2".equals(task.getCollectionScope())) {
            scopeDesc = "整个数据源";
        } else {
            scopeDesc = "未知类型(" + task.getCollectionScope() + ")";
        }
        safeLog(instanceId, taskId, "开始解析采集范围，采集范围类型=" + scopeDesc);

        safeLog(instanceId, taskId, String.format("任务执行参数汇总：采集范围=%s，数据源ID=%s，来源系统=%s(%s)", scopeDesc, task.getDatasourceId(), task.getSourceSystemName(), task.getSourceSystemId()));

        // 1. 根据采集范围，获取“库级”范围
        List<CatalogTaskScopeDO> databaseScopes;
        if ("2".equalsIgnoreCase(task.getCollectionScope())) {
            // 全量：实时从数据源加载数据库
            databaseScopes = loadDatabaseScopesFromDatasource(task, instance, datasource);
        } else {
            // 增量：直接使用任务配置的采集范围
            databaseScopes = loadDatabaseScopesFromTask(task, instance);
        }
        safeLog(instanceId, taskId, "数据库范围解析完成，共需处理数据库数量：" + databaseScopes.size());

        if (CollectionUtils.isEmpty(databaseScopes)) {
            safeLog(instanceId, taskId, "未获取到任何数据库范围，任务结束");
            return null;
        }

        // 2. 库级比对（是否新增 / 变更 / 删除）
        List<CatalogDbSaveReqVO> dbReqDTOList = compareAndRecordDatabaseScope(task, instance, databaseScopes, datasource);

        List<CatalogDbRespVO> CatalogDbByTaskId = CatalogDbService.getCatalogDbByTaskId(taskId);


        Long addCount = 0L;
        Long delCount = 0L;
        Long updateCount = 0L;

        Long totalCount = 0L;
        Long successCount = 0L;
        List<Long> updateTableIds = new ArrayList<>();
        String blacklist = task.getBlacklist();
        safeLog(instanceId, taskId,
                String.format("【任务配置】数据库/表黑名单：%s",
                        StringUtils.defaultIfBlank(blacklist, "空")));

        int dbIndex = 1;
        // 3. 循环每个库
        for (CatalogDbSaveReqVO dbScope : dbReqDTOList) {
            String dbName = dbScope.getDbName();
            safeLog(instanceId, taskId, String.format("【数据库 %d/%d】开始处理：db=%s%s", dbIndex++, databaseScopes.size(), dbName, StringUtils.isNotBlank(dbScope.getSchemaName()) ? ", schema=" + dbScope.getSchemaName() : ""));

            //黑名单
            if (StringUtils.isNotEmpty(blacklist)) {
                if (isInBlacklist(dbName, blacklist)) {
                    safeLog(instanceId, taskId,
                            String.format("【数据库跳过】db=%s 命中黑名单：%s",
                                    dbName,
                                    StringUtils.defaultIfBlank(blacklist, "空")));
                    continue;
                }
                safeLog(instanceId, taskId,
                        String.format("【数据库通过】db=%s 未命中黑名单%s",
                                dbName,
                                StringUtils.isBlank(blacklist) ? "（黑名单为空）" : ""));
            }

            CatalogDbRespVO matchedDb = findMatchedDb(dbScope, datasource, CatalogDbByTaskId);
            if (matchedDb == null) {
                Long CatalogDbId = CatalogDbTxService.createDbAndCommit(dbScope);
                dbScope.setId(CatalogDbId);
            } else {
                dbScope.setId(matchedDb.getId());
            }

            DbQueryContext dbQuery = createDbQueryForScope(datasource, dbScope, task, instance);
            try {
                TableProcessResult tableProcessResult = executeSingleDatabase(dbQuery, task, instance, dbScope, datasource);

                if (tableProcessResult != null) {
                    addCount = addCount + tableProcessResult.getAddCount();
                    updateCount = updateCount + tableProcessResult.getUpdateCount();
                    delCount = delCount + tableProcessResult.getDelCount();
                    totalCount = totalCount + tableProcessResult.getTotalCount();
                    successCount = successCount + tableProcessResult.getSuccessCount();
                    if (tableProcessResult.getUpdateTableIds().size() > 0) {
                        updateTableIds.addAll(tableProcessResult.getUpdateTableIds());
                    }
                }
            } finally {
                closeDbQuerySafely(dbQuery, task, instance, dbScope);
            }
        }

        // 更新负责人、负责部门
        CatalogTableTxService.runInNewTx(() -> this.updateResponsibleInfoForMetadata(task));

        //
        List<CatalogDbRespVO> dbsOnlyInResp = findDbsOnlyInResp(databaseScopes, datasource, CatalogDbByTaskId);
        if (CollectionUtils.isNotEmpty(dbsOnlyInResp)) {

            if (StringUtils.isNotEmpty(blacklist)) {
                dbsOnlyInResp.removeIf(db -> {
                    String dbName = db.getDbName();
                    boolean hit = isInBlacklist(dbName, blacklist);
                    if (hit) {
                        safeLog(instanceId, taskId,
                                String.format("【RESP库删除】db=%s 命中黑名单：%s",
                                        dbName,
                                        StringUtils.defaultIfBlank(blacklist, "空")));
                    }
                    return hit;
                });
            }

            List<Long> collect = CatalogDbByTaskId.stream().map(a -> a.getId()).collect(Collectors.toList());

            List<CatalogTableRespVO> CatalogTableByDbId = CatalogTableService.getCatalogTableByDbId(collect);
            if (CollectionUtils.isNotEmpty(CatalogTableByDbId)) {
                delCount = delCount + CatalogTableByDbId.size();
            }

            List<Long> tableIds = CatalogTableByDbId.stream().map(a -> a.getId()).collect(Collectors.toList());
            CatalogTableService.removeCatalogTable(tableIds);

            CatalogDbService.removeCatalogDb(collect);
        }
        return new TableProcessResult(addCount, delCount, updateCount, totalCount, successCount, updateTableIds);

    }

    private Void updateResponsibleInfoForMetadata(CatalogTaskRespVO task) {
        Long leader = task.getLeader();
        Long dept = task.getResponsibleDept();
        LambdaUpdateWrapper<CatalogDbDO> updateDbWrapper = new LambdaUpdateWrapper<>();
        updateDbWrapper.eq(CatalogDbDO::getTaskId, task.getId());

        LambdaUpdateWrapper<CatalogTableDO> updateTbWrapper = new LambdaUpdateWrapper<>();
        updateTbWrapper.eq(CatalogTableDO::getTaskId, task.getId());

        LambdaUpdateWrapper<CatalogColumnDO> updateColWrapper = new LambdaUpdateWrapper<>();
        updateColWrapper.eq(CatalogColumnDO::getTaskId, task.getId());

        boolean hasUpdate = false;
        if (leader != null) {
            updateDbWrapper.set(CatalogDbDO::getBusinessLeader, leader);
            updateTbWrapper.set(CatalogTableDO::getBusinessLeader, leader);
            updateColWrapper.set(CatalogColumnDO::getBusinessLeader, leader);
            hasUpdate = true;
        }
        if (dept != null) {
            updateDbWrapper.set(CatalogDbDO::getResponsibleDept, dept);
            updateTbWrapper.set(CatalogTableDO::getResponsibleDept, dept);
            updateColWrapper.set(CatalogColumnDO::getResponsibleDept, dept);
            hasUpdate = true;
        }

        if (hasUpdate) {
            CatalogDbMapper.update(null, updateDbWrapper);
            CatalogTableMapper.update(null, updateTbWrapper);
            CatalogColumnMapper.update(null, updateColWrapper);
        }
        return null;
    }

    private List<CatalogDbRespVO> findDbsOnlyInResp(List<CatalogTaskScopeDO> databaseScopes, AssetsDatasourceRespDTO datasource, List<CatalogDbRespVO> CatalogDbByTaskId) {

        List<CatalogDbRespVO> result = new ArrayList<>();
        if (CollectionUtils.isEmpty(CatalogDbByTaskId)) {
            return result;
        }

        for (CatalogDbRespVO resp : CatalogDbByTaskId) {
            boolean exists = false;

            if (CollectionUtils.isNotEmpty(databaseScopes)) {
                for (CatalogTaskScopeDO scope : databaseScopes) {
                    if (Objects.equals(resp.getIp(), datasource.getIp()) && Objects.equals(resp.getPort(), datasource.getPort() == null ? null : datasource.getPort()
                                                                                                                                                 .intValue()) && Objects.equals(resp.getDatasourceConfig(), datasource.getDatasourceConfig()) && Objects.equals(resp.getDbType(), datasource.getDatasourceType()) && Objects.equals(resp.getDbName(), scope.getDbName()) && Objects.equals(resp.getSchemaName(), scope.getSchemaName())) {
                        exists = true;
                        break;
                    }
                }
            }

            if (!exists) {
                result.add(resp);
            }
        }
        return result;
    }

    private CatalogDbRespVO findMatchedDb(CatalogDbSaveReqVO dbScope, AssetsDatasourceRespDTO datasource, List<CatalogDbRespVO> CatalogDbByTaskId) {

        if (dbScope == null || CollectionUtils.isEmpty(CatalogDbByTaskId)) {
            return null;
        }

        for (CatalogDbRespVO resp : CatalogDbByTaskId) {
            if (Objects.equals(resp.getIp(), datasource.getIp()) && Objects.equals(resp.getPort(), datasource.getPort() == null ? null : datasource.getPort()
                                                                                                                                         .intValue()) && Objects.equals(resp.getDatasourceConfig(), datasource.getDatasourceConfig()) && Objects.equals(resp.getDbType(), datasource.getDatasourceType()) && Objects.equals(resp.getDbName(), dbScope.getDbName()) && Objects.equals(resp.getSchemaName(), dbScope.getSchemaName())) {
                return resp;
            }
        }
        return null;
    }

    private DbQueryContext createDbQueryForScope(AssetsDatasourceRespDTO datasource, CatalogDbSaveReqVO dbScope, CatalogTaskRespVO task, CatalogTaskInstanceDO instance) {
        DbQueryProperty property = new DbQueryProperty(datasource.getDatasourceType(), datasource.getIp(), datasource.getPort(), datasource.getDatasourceConfig());

        // PG / Kingbase 切库 + schema
        if (DbType.KINGBASE8.getDb().equals(property.getDbType()) || DbType.POSTGRE_SQL.getDb()
                .equals(property.getDbType())) {
            property.setDbName(dbScope.getDbName());
            property.setSid(dbScope.getSchemaName());
        }

        DbQuery dbQuery = dataSourceFactory.createDbQuery(property);
        if (!dbQuery.valid()) {
            safeLog(instance.getId(), task.getId(), "数据库连接失败，db=" + dbScope.getDbName());
            throw new DataQueryException("数据库连接失败");
        }

        property.setDbName(dbScope.getDbName());
        property.setSid(dbScope.getSchemaName());
        return new DbQueryContext(dbQuery, property);
    }

    private TableProcessResult executeSingleDatabase(DbQueryContext dbQuery, CatalogTaskRespVO task, CatalogTaskInstanceDO instance, CatalogDbSaveReqVO dbScope, AssetsDatasourceRespDTO datasource) {
        String blacklist = task.getBlacklist();
        boolean notEmptyBlacklist = StringUtils.isNotEmpty(blacklist);
        Long taskId = task.getId();
        Long instanceId = instance.getId();

        Long addCount = 0L;
        Long delCount = 0L;
        Long updateCount = 0L;

        Long totalCount = 0L;
        Long successCount = 0L;


        safeLog(instanceId, taskId, "开始处理数据库：" + dbScope.getDbName() + (StringUtils.isNotBlank(dbScope.getSchemaName()) ? "，schema=" + dbScope.getSchemaName() : ""));

        safeLog(instanceId, taskId, String.format("[DB] 当前计数快照：新增=%d，更新=%d，删除=%d", addCount, updateCount, delCount));

        // 1. 表列表（不再建连接）
        List<DbTable> tables = loadTablesByDatabase(dbQuery, task, instance, dbScope);
        safeLog(instanceId, taskId, String.format("数据库 %s 表加载完成，表数量=%d", dbScope.getDbName(), tables.size()));
        if (CollectionUtils.isEmpty(tables)) {
            return null;
        }
        int size = tables.size();
        safeLog(instanceId, taskId, String.format("[DB] 表加载完成：db=%s%s，表数量=%d", dbScope.getDbName(), StringUtils.isNotBlank(dbScope.getSchemaName()) ? ", schema=" + dbScope.getSchemaName() : "", size));
        totalCount = size + totalCount;
        List<CatalogTableRespVO> tableRespDTOList = getCatalogTableById(task, instance, dbScope);

        // 2. 表级比对
        List<CatalogTableSaveReqVO> CatalogTables = compareAndRecordTables(task, instance, dbScope, tables);


        safeLog(instanceId, taskId, String.format("[DB] 开始处理表列表，共 %d 张表", CatalogTables.size()));


        List<DbColumn> columns = loadColumnsByTable(dbQuery, task, instance, dbScope);


        safeLog(instanceId, taskId, String.format("库 %s 字段加载完成，字段数量=%d", dbScope.getDbName(), columns.size()));


        Map<String, List<DbColumn>> tableColumnMap = columns.stream()
                .collect(Collectors.groupingBy(DbColumn::getTableName));

        List<CatalogColumnSaveReqVO> CatalogColumnReqDTOList = new ArrayList<>();
        List<Long> updateTableIds = new ArrayList<>();
        // 3. 表循环
        for (CatalogTableSaveReqVO table : CatalogTables) {
            if (notEmptyBlacklist) {
                String dbName = dbScope.getDbName();
                String tableName = table.getTableName();
                String fullTableName = dbName + "." + tableName;
                if (isInBlacklist(fullTableName, blacklist)) {
                    safeLog(instanceId, taskId,
                            String.format("【表跳过】table=%s 命中黑名单：%s",
                                    fullTableName,
                                    StringUtils.defaultIfBlank(blacklist, "空")));
                    continue;
                }

                safeLog(instanceId, taskId,
                        String.format("【表通过】table=%s 未命中黑名单%s",
                                fullTableName,
                                StringUtils.isBlank(blacklist) ? "（黑名单为空）" : ""));

            }

            List<DbColumn> dbColumns = tableColumnMap.get(table.getTableName());
            final List<DbColumn> finalDbColumns;
            if (DbType.HIVE.getDb().equals(dbQuery.getProperty().getDbType())) {
                finalDbColumns = dbQuery.getDbQuery().getTableColumns(dbQuery.getProperty(), table.getTableName());
            } else {
                finalDbColumns = dbColumns;
            }

            if (CollectionUtils.isEmpty(finalDbColumns)) {
                safeLog(instanceId, taskId, "[TABLE] 单表处理失败，已回滚，table=" + table.getTableName() + "，原因：字段未获取到");
                continue;
            }

            try {

                TableProcessResult result = CatalogTableTxService.runInNewTx(() -> doProcessSingleTable(task, instance, dbScope, table, tableRespDTOList, finalDbColumns));
//                TableProcessResult result = doProcessSingleTable(task, instance, dbScope, table, tableRespDTOList, dbColumns);
                //
                if (result != null) {
                    addCount = addCount + result.getAddCount();
                    updateCount = updateCount + result.getUpdateCount();
                    successCount = successCount + result.getSuccessCount();
                    CatalogColumnReqDTOList.addAll(result.getCatalogColumnReqList());
                    if (result.getUpdateCount() != 0 || result.getAddCount() != 0) {
                        updateTableIds.add(table.getId());
                    }
                }

            } catch (Exception e) {
                safeLog(instanceId, taskId, "[TABLE] 单表处理失败，已回滚，table=" + table.getTableName() + "，原因：" + e.getMessage());
            }
        }

        //根据表存储大小之和更新库存储大小
        CatalogTableTxService.runInNewTx(() -> CatalogDbMapper.updateStorageSizeById(dbScope.getId()));
        //根据表的行数之和更新库的数据行数
        CatalogTableTxService.runInNewTx(() -> CatalogDbMapper.updateDataRowCountById(dbScope.getId()));

        if (CollectionUtils.isNotEmpty(CatalogColumnReqDTOList)) {
            List<CatalogColumnDO> columnDOList = CatalogTableTxService.runInNewTx(() -> CatalogColumnService.createCatalogColumnList(CatalogColumnReqDTOList));

            //添加字段变更版本日志记录
            CatalogTableTxService.runInNewTx(() -> CatalogColumnLogService.createCatalogColumnLog(columnDOList));
        }
        //更新元数据表的字段数
        CatalogTableTxService.runInNewTx(() -> CatalogDbMapper.updateColumnCountByDbId(dbScope.getId()));


        List<CatalogTableRespVO> tablesOnlyInResp = findTablesOnlyInResp(CatalogTables, tableRespDTOList);
        if (notEmptyBlacklist) {
            Iterator<CatalogTableRespVO> iterator = tablesOnlyInResp.iterator();
            while (iterator.hasNext()) {
                CatalogTableRespVO table = iterator.next();

                String dbName = table.getDbName();      // 确认这里字段名
                String tableName = table.getTableName();
                String fullName = dbName + "." + tableName;

                // 黑名单判断
                if (isInBlacklist(fullName, blacklist)) {
                    safeLog(instanceId, taskId,
                            String.format("【RESP表删除】table=%s 命中黑名单：%s",
                                    fullName,
                                    StringUtils.defaultIfBlank(blacklist, "空")));

                    iterator.remove(); // 安全删除
                }
            }
        }

        delCount = delCount + tablesOnlyInResp.size();
        if (CollectionUtils.isNotEmpty(tablesOnlyInResp)) {
            safeLog(instanceId, taskId, String.format("[DB] 发现待删除表数量=%d", tablesOnlyInResp.size()));
            for (CatalogTableRespVO resp : tablesOnlyInResp) {
                safeLog(instanceId, taskId, "[TABLE] 表未在本次采集中出现，标记为删除：" + resp.getTableName());
            }
            List<Long> collect = tablesOnlyInResp.stream().map(a -> a.getId()).collect(Collectors.toList());
            CatalogTableService.removeCatalogTable(collect);
        }
        safeLog(instanceId, taskId, String.format("[DB] 数据库处理完成：db=%s，新增表=%d，更新表=%d，删除表=%d，成功表=%d", dbScope.getDbName(), addCount, updateCount, delCount, successCount));


        return new TableProcessResult(addCount, delCount, updateCount, totalCount, successCount, updateTableIds);
    }

    private TableProcessResult doProcessSingleTable(CatalogTaskRespVO task, CatalogTaskInstanceDO instance, CatalogDbSaveReqVO dbScope, CatalogTableSaveReqVO table, List<CatalogTableRespVO> tableRespDTOList, List<DbColumn> columns) {
        Long taskId = task.getId();
        Long instanceId = instance.getId();
        Long addCount = 0L;
        Long updateCount = 0L;
        Long successCount = 0L;

        safeLog(instanceId, taskId, "开始处理表：" + dbScope.getDbName() + "." + table.getTableName());

        safeLog(instanceId, taskId, String.format("表 %s 字段加载完成，字段数量=%d", table.getTableName(), columns.size()));

        if (CollectionUtils.isEmpty(columns)) {
            return null;
        }

        List<CatalogColumnSaveReqVO> columnReqDTOS = compareAndRecordColumns(task, instance, dbScope, table, columns);

        safeLog(instanceId, taskId, String.format("[COLUMN] 表 %s 字段加载完成，字段数量=%d", table.getTableName(), columns.size()));

        CatalogTableRespVO matched = findMatchedTable(table, tableRespDTOList);

        if (matched != null) {

            safeLog(instanceId, taskId, "[TABLE] 表已存在，进入结构比对：" + table.getTableName());

            table.setId(matched.getId());

            List<CatalogColumnRespVO> CatalogColumnRespDTOList = getCatalogColumnByTaskId(table, instance, dbScope);

            boolean updated = isTableUpdated(table, matched, columnReqDTOS, CatalogColumnRespDTOList);
            boolean updated2 = isTableUpdated2(table, matched);
            if (updated || updated2) {

                safeLog(instanceId, taskId, "[TABLE] 表结构发生变更，执行更新：" + table.getTableName());

                updateCount++;//11

                CatalogTableTxService.runInNewTx(() -> CatalogTableService.updateCatalogTable(table));
//                CatalogTableService.updateCatalogTable(table);

                safeLog(instanceId, taskId, "[TABLE] 表已更新，准备删除并重建字段：" + table.getTableName());

                removeCatalogColumn(table, instance, dbScope);
            } else {
                safeLog(instanceId, taskId, "[TABLE] 表结构未变化，跳过更新：" + table.getTableName());
                successCount++;
                return new TableProcessResult(addCount, updateCount, successCount, new ArrayList<>());
            }

        } else {

            safeLog(instanceId, taskId, "[TABLE] 新表发现，准备创建元数据表：" + table.getTableName());

            Long CatalogTableId = CatalogTableTxService.runInNewTx(() -> CatalogTableService.createCatalogTable(table));
//            Long CatalogTableId = CatalogTableService.createCatalogTable(table);

            safeLog(instanceId, taskId, "[TABLE] 新表创建完成，CatalogTableId=" + CatalogTableId + "，table=" + table.getTableName());

            table.setId(CatalogTableId);
            addCount++;
            //新增的表所有的字段都设置版本为1
            columnReqDTOS.forEach(columnReqDTO -> columnReqDTO.setVersion(1));
        }
        //添加表的版本变更记录
        Long CatalogTableLogId = CatalogTableTxService.runInNewTx(() -> CatalogTableLogService.createCatalogTableLog(table));

        for (CatalogColumnSaveReqVO columnReqDTO : columnReqDTOS) {
            columnReqDTO.setTableId(table.getId());
            columnReqDTO.setCatalogTableLogId(CatalogTableLogId);
            columnReqDTO.setTbPartitionKey(table.getPartitionKey());
        }

        safeLog(instanceId, taskId, String.format("[COLUMN] 表 %s 字段处理完成，创建字段数=%d", table.getTableName(), columnReqDTOS.size()));

        successCount++;


        return new TableProcessResult(addCount, updateCount, successCount, columnReqDTOS);
    }


    private void removeCatalogColumn(CatalogTableSaveReqVO table, CatalogTaskInstanceDO instance, CatalogDbSaveReqVO dbScope) {

        CatalogColumnRespVO createReqVO = new CatalogColumnRespVO();
        createReqVO.setTaskId(table.getTaskId());
        createReqVO.setTableId(table.getId());
        CatalogColumnService.removeCatalogColumn(createReqVO);
    }

    private boolean isTableUpdated(CatalogTableSaveReqVO reqTable, CatalogTableRespVO respTable, List<CatalogColumnSaveReqVO> reqColumns, List<CatalogColumnRespVO> respColumns) {

        // 1️⃣ 表注释不一致 → 更新
        boolean result = false;
        StringBuilder updateMsg = new StringBuilder();
        Set<String> type = new HashSet<>();//变更类型集合
        String reqComment = StringUtils.defaultString(reqTable.getTableComment());
        String respComment = StringUtils.defaultString(respTable.getTableComment());
        if (!reqComment.equals(respComment)) {
            result = true;
            updateMsg.append("表注释变更旧注释：")
                    .append(respComment)
                    .append("，新注释：")
                    .append(reqComment)
                    .append("；\n");
            type.add("1");
        }

        // 2️⃣ 字段数量不一致 → 更新
        int reqSize = reqColumns == null ? 0 : reqColumns.size();
        int respSize = respColumns == null ? 0 : respColumns.size();
        if (reqSize != respSize) {
            result = true;
        }

        // 3️⃣ 构建 respColumns 的 Map（columnName 唯一）
        Map<String, CatalogColumnRespVO> respColumnMap = new HashMap<>();
        if (respColumns != null) {
            for (CatalogColumnRespVO respCol : respColumns) {
                respColumnMap.put(respCol.getColumnName(), respCol);
            }
        }
        Set<String> addColumnNames = new HashSet<>(), updateColumnNames = new HashSet<>(), deleteColumnNames = new HashSet<>();
        // 4️⃣ 循环 reqColumns，逐字段判断
        if (reqColumns != null) {
            for (CatalogColumnSaveReqVO reqCol : reqColumns) {

                CatalogColumnRespVO respCol = respColumnMap.get(reqCol.getColumnName());

                // 字段不存在 → 更新
                if (respCol == null) {
                    result = true;
                    reqCol.setVersion(1);
                    reqCol.setUpdateType("1");
                    reqCol.setUpdateMsg("新增字段");
                    addColumnNames.add(reqCol.getColumnName());
                }

                // 字段属性不一致 → 更新
                if (respCol != null && isColumnUpdated(reqCol, respCol)) {
                    result = true;
                    updateColumnNames.add(reqCol.getColumnName());
                }
            }
        }

        // 5 检查删除的字段：遍历原有的字段，看是否在新的字段列表中存在
        if (respColumns != null) {
            for (CatalogColumnRespVO respCol : respColumns) {
                boolean found = false;
                if (reqColumns != null) {
                    for (CatalogColumnSaveReqVO reqCol : reqColumns) {
                        if (respCol.getColumnName().equals(reqCol.getColumnName())) {
                            found = true;
                            break;
                        }
                    }
                }
                if (!found) {
                    result = true;
                    deleteColumnNames.add(respCol.getColumnName());
                }
            }
        }

        if (!addColumnNames.isEmpty()) {
            type.add("2");
            updateMsg.append("新增：")
                    .append(addColumnNames.size())
                    .append("个字段：")
                    .append(String.join(",", addColumnNames))
                    .append("；\n");
        }
        if (!deleteColumnNames.isEmpty()) {
            type.add("2");
            updateMsg.append("删除：")
                    .append(deleteColumnNames.size())
                    .append("个字段：")
                    .append(String.join(",", deleteColumnNames))
                    .append("；\n");
        }
        if (!updateColumnNames.isEmpty()) {
            type.add("2");
            updateMsg.append("更新：")
                    .append(updateColumnNames.size())
                    .append("个字段：")
                    .append(String.join(",", updateColumnNames))
                    .append("；\n");
        }

        reqTable.setUpdateMsg(updateMsg.toString());
        reqTable.setUpdateType(String.join(",", type));
        return result;
    }

    private boolean isTableUpdated2(CatalogTableSaveReqVO reqTable, CatalogTableRespVO respTable) {
        boolean result = false;
        StringBuilder updateMsg = new StringBuilder();
        Set<String> type = new HashSet<>();//变更类型集合

        // 检查索引字段是否有变更、存储大小是否变更  respTables是匹配到的表中的数据
        CatalogTableDO CatalogTableDO = BeanUtils.toBean(respTable, CatalogTableDO.class);
        // 获取表中原来存储的索引字段和存储大小
        String tbIndex = CatalogTableDO.getTbIndex();
        Integer storageSize = CatalogTableDO.getStorageSize();
        if (storageSize == null) {
            storageSize = 0;
        }
        // 获取数据库元数据信息，包括数据库类型
        CatalogDbDO CatalogDbDO = CatalogDbMapper.findById(CatalogTableDO.getDbId());
        if (CatalogDbDO != null) {
            // 使用数据库方言获取表的行数、索引、分区字段等信息
            DatabaseDialect dialect = DatabaseDialectFactory.getDialect(CatalogDbDO);
            if (dialect != null) {
                // 批量获取表元数据信息
                DatabaseDialect.TableMetadata metadata = dialect.getTableMetadata(CatalogDbDO, CatalogTableDO.getTableName());
                // 对比索引字段和存储大小
                if (StringUtils.isNotBlank(tbIndex) && !tbIndex.equals(metadata.getIndexes())) {
                    result = true;
                    updateMsg.append("表索引字段变更旧索引：")
                            .append(tbIndex)
                            .append("，新索引字段：")
                            .append(metadata.getIndexes())
                            .append("；\n");
                    type.add("3");
                }

                if (metadata.getTableSize() != null && storageSize != metadata.getTableSize().intValue()) {
                    result = true;
                    updateMsg.append("表存储大小变更旧存储大小：")
                            .append(storageSize)
                            .append("，新存储大小：")
                            .append(metadata.getTableSize())
                            .append("；\n");
                    type.add("4");
                }
            }
        }
        reqTable.setUpdateMsg(updateMsg.toString());
        reqTable.setUpdateType(String.join(",", type));
        return result;
    }

    private boolean isColumnUpdated(CatalogColumnSaveReqVO req, CatalogColumnRespVO resp) {

        StringBuilder updateMsg = new StringBuilder();
        Set<String> type = new HashSet<>();//变更类型集合
        boolean result = false;

        // 字段注释变更
        String reqComment = StringUtils.defaultString(req.getColumnComment());
        String respComment = StringUtils.defaultString(resp.getColumnComment());
        if (!reqComment.equals(respComment)) {
            result = true;
            updateMsg.append("字段注释变更旧注释：")
                    .append(respComment)
                    .append("，新注释：")
                    .append(reqComment)
                    .append("；\n");
            type.add("1");
        }

        // 字段类型变更
        String reqType = StringUtils.defaultString(req.getColumnType());
        String respType = StringUtils.defaultString(resp.getColumnType());
        if (!reqType.equals(respType)) {
            result = true;
            updateMsg.append("字段类型变更旧类型：").append(respType).append("，新类型：").append(reqType).append("；\n");
            type.add("2");
        }

        // 字段长度变更
        if (!Objects.equals(req.getColumnLength(), resp.getColumnLength())) {
            result = true;
            updateMsg.append("字段长度变更旧长度：")
                    .append(resp.getColumnLength())
                    .append("，新长度：")
                    .append(req.getColumnLength())
                    .append("；\n");
            type.add("3");
        }

        // 字段精度变更
        if (!Objects.equals(req.getColumnPrecision(), resp.getColumnPrecision())) {
            result = true;
            updateMsg.append("字段精度变更旧精度：")
                    .append(resp.getColumnPrecision())
                    .append("，新精度：")
                    .append(req.getColumnPrecision())
                    .append("；\n");
            type.add("4");
        }

        // 字段小数位数变更
        if (!Objects.equals(req.getColumnScale(), resp.getColumnScale())) {
            result = true;
            updateMsg.append("字段小数位数变更旧小数位数：")
                    .append(resp.getColumnScale())
                    .append("，新小数位数：")
                    .append(req.getColumnScale())
                    .append("；\n");
            type.add("5");
        }

        // 字段默认值变更
        String reqDefault = StringUtils.defaultString(req.getDefaultValue());
        String respDefault = StringUtils.defaultString(resp.getDefaultValue());
        if (!reqDefault.equals(respDefault)) {
            result = true;
            updateMsg.append("字段默认值变更旧默认值：")
                    .append(respDefault)
                    .append("，新默认值：")
                    .append(reqDefault)
                    .append("；\n");
            type.add("6");
        }

        // 主键标识变更
        String reqPkFlag = StringUtils.defaultString(req.getPkFlag());
        String respPkFlag = StringUtils.defaultString(resp.getPkFlag());
        if (!reqPkFlag.equals(respPkFlag)) {
            result = true;
            updateMsg.append("主键标识变更旧标识：")
                    .append(respPkFlag)
                    .append("，新标识：")
                    .append(reqPkFlag)
                    .append("；\n");
            type.add("7");
        }

        // 外键标识变更
        String reqFkFlag = StringUtils.defaultString(req.getFkFlag());
        String respFkFlag = StringUtils.defaultString(resp.getFkFlag());
        if (!reqFkFlag.equals(respFkFlag)) {
            result = true;
            updateMsg.append("外键标识变更旧标识：")
                    .append(respFkFlag)
                    .append("，新标识：")
                    .append(reqFkFlag)
                    .append("；\n");
            type.add("8");
        }

        // 可空标识变更
        String reqNullableFlag = StringUtils.defaultString(req.getNullableFlag());
        String respNullableFlag = StringUtils.defaultString(resp.getNullableFlag());
        if (!reqNullableFlag.equals(respNullableFlag)) {
            result = true;
            updateMsg.append("可空标识变更旧标识：")
                    .append(respNullableFlag)
                    .append("，新标识：")
                    .append(reqNullableFlag)
                    .append("；\n");
            type.add("9");
        }

        req.setUpdateMsg(updateMsg.toString());
        req.setUpdateType(String.join(",", type));
        //字段调整版本加1
        if (result) {
            req.setVersion(resp.getVersion() != null ? resp.getVersion() + 1 : 1);
        } else {
            req.setVersion(resp.getVersion());
        }
        return result;
    }


    private List<CatalogColumnRespVO> getCatalogColumnByTaskId(CatalogTableSaveReqVO table, CatalogTaskInstanceDO instance, CatalogDbSaveReqVO dbScope) {
        CatalogColumnRespVO createReqVO = new CatalogColumnRespVO();
        createReqVO.setTaskId(table.getTaskId());
        createReqVO.setTableId(table.getId());
        return CatalogColumnService.getCatalogColumnList(createReqVO);
    }

    private List<CatalogTableRespVO> findTablesOnlyInResp(List<CatalogTableSaveReqVO> CatalogTables, List<CatalogTableRespVO> tableRespDTOList) {

        List<CatalogTableRespVO> result = new ArrayList<>();
        if (CollectionUtils.isEmpty(tableRespDTOList)) {
            return result;
        }

        for (CatalogTableRespVO resp : tableRespDTOList) {
            boolean exists = false;
            if (CollectionUtils.isNotEmpty(CatalogTables)) {
                for (CatalogTableSaveReqVO req : CatalogTables) {
                    if (Objects.equals(req.getDbName(), resp.getDbName()) && Objects.equals(req.getSchemaName(), resp.getSchemaName()) && Objects.equals(req.getTableName(), resp.getTableName())) {
                        exists = true;
                        break;
                    }
                }
            }
            if (!exists) {
                result.add(resp);
            }
        }
        return result;
    }


    private CatalogTableRespVO findMatchedTable(CatalogTableSaveReqVO req, List<CatalogTableRespVO> tableRespDTOList) {
        if (req == null || CollectionUtils.isEmpty(tableRespDTOList)) {
            return null;
        }

        for (CatalogTableRespVO resp : tableRespDTOList) {
            if (Objects.equals(req.getDbId(), resp.getDbId()) && Objects.equals(req.getTableName(), resp.getTableName())) {
                return resp;
            }
        }
        return null;
    }


    private List<CatalogTableRespVO> getCatalogTableById(CatalogTaskRespVO task, CatalogTaskInstanceDO instance, CatalogDbSaveReqVO dbScope) {
        CatalogTableRespVO CatalogTableReqDTO = new CatalogTableRespVO();
        CatalogTableReqDTO.setTaskId(task.getId());
        CatalogTableReqDTO.setDbId(dbScope.getId());
        return CatalogTableService.getCatalogTableById(CatalogTableReqDTO);
    }

    private List<DbTable> loadTablesByDatabase(DbQueryContext dbQuery, CatalogTaskRespVO task, CatalogTaskInstanceDO instance, CatalogDbSaveReqVO dbScope) {

        try {
            List<DbTable> tables = dbQuery.getDbQuery().getTables(dbQuery.getProperty());
            return tables == null ? new ArrayList<>() : tables;
        } catch (Exception e) {
            safeLog(instance.getId(), task.getId(), "加载表异常，db=" + dbScope.getDbName() + "，原因：" + e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<DbColumn> loadColumnsByTable(DbQueryContext dbQuery, CatalogTaskRespVO task, CatalogTaskInstanceDO instance, CatalogDbSaveReqVO dbScope) {

        try {
            List<DbColumn> tableColumns = dbQuery.getDbQuery().getDbColumns(dbQuery.getProperty());
            return tableColumns == null ? new ArrayList<>() : tableColumns;
        } catch (Exception e) {
            safeLog(instance.getId(), task.getId(), "加载字段异常，库=" + dbScope.getDbName() + "，原因：" + e.getMessage());
            return new ArrayList<>();
        }
    }

    private void closeDbQuerySafely(DbQueryContext ctx, CatalogTaskRespVO task, CatalogTaskInstanceDO instance, CatalogDbSaveReqVO dbScope) {
        try {
            ctx.getDbQuery().close();
        } catch (Exception e) {
            safeLog(instance.getId(), task.getId(), "关闭数据库连接异常，db=" + dbScope.getDbName() + "，原因：" + e.getMessage());
        }
    }


    private List<CatalogTaskScopeDO> loadDatabaseScopesFromDatasource(CatalogTaskRespVO task, CatalogTaskInstanceDO instance, AssetsDatasourceRespDTO datasource) {

        Long taskId = task.getId();
        Long instanceId = instance.getId();

        safeLog(instanceId, taskId, "全量模式：从数据源加载数据库信息");

        // 1. 构建 DbQueryProperty
        DbQueryProperty baseProperty = new DbQueryProperty(datasource.getDatasourceType(), datasource.getIp(), datasource.getPort(), datasource.getDatasourceConfig());

        // 2. 获取数据库列表（含层级）
        List<DbName> dbNames;
        DbQuery rootQuery = dataSourceFactory.createDbQuery(baseProperty);
        try {
            if (!rootQuery.valid()) {
                safeLog(instanceId, taskId, "数据库连接失败");
                throw new DataQueryException("数据库连接失败");
            }
            dbNames = rootQuery.getDbNames(null);
        } finally {
            rootQuery.close();
        }

        List<CatalogTaskScopeDO> scopeList = new ArrayList<>();
        if (CollectionUtils.isEmpty(dbNames)) {
            safeLog(instanceId, taskId, "未获取到任何数据库");
            return scopeList;
        }

        // 3. 单层结构：直接映射 dbName
        if (dbNames.get(0).getLevel() == 1 && dbNames.get(0).getTotalLevels() == 1) {
            for (DbName dbName : dbNames) {
                CatalogTaskScopeDO scope = new CatalogTaskScopeDO();
                scope.setDbName(dbName.getDbName());
                scopeList.add(scope);
            }
            return scopeList;
        }

        // 4. 多层结构：加载下级并映射 db + schema
        for (DbName dbName : dbNames) {

            DbQueryProperty childProperty = baseProperty;
            if (DbType.KINGBASE8.getDb().equals(baseProperty.getDbType()) || DbType.POSTGRE_SQL.getDb()
                    .equals(baseProperty.getDbType())) {

                childProperty = baseProperty.copy();
                childProperty.setDbName(dbName.getDbName());
            }

            DbQuery childQuery = dataSourceFactory.createDbQuery(childProperty);
            try {
                if (!childQuery.valid()) {
                    continue;
                }
                List<DbName> children = childQuery.getDbNames(dbName);
                dbName.setChildren(children);
            } catch (Exception e) {
                safeLog(instanceId, taskId, "获取数据库下级失败，db=" + dbName.getDbName() + "，原因：" + e.getMessage());
            } finally {
                childQuery.close();
            }

            List<DbName> children = dbName.getChildren();
            if (CollectionUtils.isNotEmpty(children)) {
                for (DbName child : children) {
                    CatalogTaskScopeDO scope = new CatalogTaskScopeDO();
                    scope.setDbName(dbName.getDbName());
                    scope.setSchemaName(child.getDbName());
                    scopeList.add(scope);
                }
            } else {
                CatalogTaskScopeDO scope = new CatalogTaskScopeDO();
                scope.setDbName(dbName.getDbName());
                scopeList.add(scope);
            }
        }

        safeLog(instanceId, taskId, "数据库加载完成，范围数量：" + scopeList.size());
        return scopeList;
    }


    private List<CatalogTaskScopeDO> loadDatabaseScopesFromTask(CatalogTaskRespVO task, CatalogTaskInstanceDO instance) {

        safeLog(instance.getId(), task.getId(), "增量模式：使用任务配置的采集范围");

        return task.getScopeSaveReqVOS();
    }

    private List<CatalogDbSaveReqVO> compareAndRecordDatabaseScope(CatalogTaskRespVO task, CatalogTaskInstanceDO instance, List<CatalogTaskScopeDO> databaseScopes, AssetsDatasourceRespDTO datasource) {
        List<CatalogDbSaveReqVO> dbReqDTOList = new ArrayList<>();
        String creatorId = redisService.get(buildRunLockKey(task.getId()) + ":creatorId");
        String createBy = redisService.get(buildRunLockKey(task.getId()) + ":createBy");
        //TODO 逻辑待完善
        for (CatalogTaskScopeDO databaseScope : databaseScopes) {

            CatalogDbSaveReqVO createReqVO = new CatalogDbSaveReqVO();
            //采集标识
            createReqVO.setTaskId(task.getId());

            // ====== 来源系统 ======
            createReqVO.setSourceSystemId(task.getSourceSystemId());
            createReqVO.setSourceSystemName(task.getSourceSystemName());

            // ====== 数据源基础信息 ======
            createReqVO.setDatasourceId(datasource.getId());
            createReqVO.setDbType(datasource.getDatasourceType());
            createReqVO.setIp(datasource.getIp());
            createReqVO.setPort(datasource.getPort() != null ? datasource.getPort().intValue() : null);
            createReqVO.setDatasourceConfig(datasource.getDatasourceConfig());
            createReqVO.setBelongingSystem(datasource.getDatasourceName());

            createReqVO.setCreateBy(StringUtils.isNotEmpty(createBy) ? createBy : "System Collection Task");
            createReqVO.setCreatorId(StringUtils.isNotEmpty(creatorId) ? Long.parseLong(creatorId) : 1L);

            // ====== 库 / 模式 ======
            createReqVO.setDbName(databaseScope.getDbName());
            createReqVO.setSchemaName(databaseScope.getSchemaName());

            // ====== 描述 ======
            createReqVO.setDescription(databaseScope.getDescription());

            // ====== 状态与标志位（后端可统一兜底，这里显式给） ======
            createReqVO.setStatus("0");      // 未发布
            createReqVO.setAuditStatus("2");
            createReqVO.setVersion(1);
            createReqVO.setAuditTime(new Date());

            dbReqDTOList.add(createReqVO);
        }

        return dbReqDTOList;
    }

    private List<CatalogTableSaveReqVO> compareAndRecordTables(CatalogTaskRespVO task, CatalogTaskInstanceDO instance, CatalogDbSaveReqVO dbScope, List<DbTable> tables) {
        List<CatalogTableSaveReqVO> CatalogTableReqDTOList = new ArrayList<>();
        String creatorId = redisService.get(buildRunLockKey(task.getId()) + ":creatorId");
        String createBy = redisService.get(buildRunLockKey(task.getId()) + ":createBy");

        for (DbTable table : tables) {

            CatalogTableSaveReqVO CatalogTableReqDTO = new CatalogTableSaveReqVO();

            // ====== 关联关系 ======
            CatalogTableReqDTO.setTaskId(task.getId());
            CatalogTableReqDTO.setDbId(dbScope.getId());
            CatalogTableReqDTO.setDatasourceId(task.getDatasourceId());

            // ====== 表基础信息 ======
            CatalogTableReqDTO.setTableName(table.getTableName());
            CatalogTableReqDTO.setTableComment(StringUtils.isEmpty(table.getTableComment()) ? "" : table.getTableComment());

            // ====== 库 / 模式 ======
            CatalogTableReqDTO.setDbName(dbScope.getDbName());
            CatalogTableReqDTO.setSchemaName(dbScope.getSchemaName());

            CatalogTableReqDTO.setCreateBy(StringUtils.isNotEmpty(createBy) ? createBy : "System Collection Task");
            CatalogTableReqDTO.setCreatorId(StringUtils.isNotEmpty(creatorId) ? Long.parseLong(creatorId) : 1L);

            // ====== 状态与标志位 ======
            CatalogTableReqDTO.setStatus("0");     // 未发布
            CatalogTableReqDTO.setVersion(1);
            CatalogTableReqDTO.setMasterFlag("1");
            CatalogTableReqDTO.setTempFlag("0");
            CatalogTableReqDTO.setAuditStatus("2");
            CatalogTableReqDTO.setAuditTime(new Date());

            // ====== 描述 ======
            CatalogTableReqDTO.setDescription(table.getTableComment());

            // ====== 调用元数据服务 ======
//            Long CatalogTableId = CatalogTableService.createCatalogTable(CatalogTableReqDTO);
//
//            CatalogTableReqDTO.setId(CatalogTableId);
            CatalogTableReqDTOList.add(CatalogTableReqDTO);
        }

        return CatalogTableReqDTOList;
    }

    private List<CatalogColumnSaveReqVO> compareAndRecordColumns(CatalogTaskRespVO task, CatalogTaskInstanceDO instance, CatalogDbSaveReqVO dbScope, CatalogTableSaveReqVO table, List<DbColumn> columns) {

        List<CatalogColumnSaveReqVO> columnReqDTOS = new ArrayList<>();
        String creatorId = redisService.get(buildRunLockKey(task.getId()) + ":creatorId");
        String createBy = redisService.get(buildRunLockKey(task.getId()) + ":createBy");
        for (DbColumn column : columns) {
            if (null != column) {
                CatalogColumnSaveReqVO createReqVO = new CatalogColumnSaveReqVO();

                // ====== 关联信息 ======
                createReqVO.setTaskId(task.getId());
                createReqVO.setDbId(dbScope.getId());
                createReqVO.setTableId(table.getId());
                createReqVO.setDatasourceId(task.getDatasourceId());

                // ====== 字段基础信息 ======
                createReqVO.setColumnName(StringUtils.isEmpty(column.getColName()) ? "" : column.getColName());
                createReqVO.setColumnComment(StringUtils.isEmpty(column.getColComment()) ? "" : column.getColComment());
                createReqVO.setColumnType(StringUtils.isEmpty(column.getDataType()) ? "" : column.getDataType());

                // ====== 长度 / 精度 ======
                createReqVO.setColumnLength(parseInt(column.getDataLength()));
                createReqVO.setColumnPrecision(parseInt(column.getDataPrecision()));
                createReqVO.setColumnScale(parseInt(column.getDataScale()));

                // ====== 默认值 ======
                createReqVO.setDefaultValue(column.getDataDefault());

                // ====== 主键 / 可空 ======
                createReqVO.setPkFlag(Boolean.TRUE.equals(column.getColKey()) ? "1" : "0");
                createReqVO.setNullableFlag(Boolean.FALSE.equals(column.getNullable()) ? "1" : "0");
                createReqVO.setFkFlag("0");

                createReqVO.setCreateBy(StringUtils.isNotEmpty(createBy) ? createBy : "System Collection Task");
                createReqVO.setCreatorId(StringUtils.isNotEmpty(creatorId) ? Long.parseLong(creatorId) : 1L);

                // ====== 状态与标志位 ======
                createReqVO.setStatus("0");     // 未发布
                createReqVO.setVersion(1);
                createReqVO.setAuditStatus("2");
                createReqVO.setAuditTime(new Date());

                // ====== 描述 ======
                createReqVO.setDescription(column.getColComment());


                columnReqDTOS.add(createReqVO);
                // ====== 调用字段元数据服务 ======

                // 如需回写 columnId，可在 DbColumn 中扩展字段
            }
        }
        return columnReqDTOS;
    }

    /**
     * 安全的 String -> Integer 转换
     */
    private Integer parseInt(String val) {
        if (val == null || val.trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.valueOf(val.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void fillProjectRelation(CatalogTaskDO taskDO) {
        if (taskDO == null) {
            return;
        }

        if ((taskDO.getProjectId() == null || StringUtils.isEmpty(taskDO.getProjectCode()))
                && taskDO.getId() != null) {
            CatalogTaskDO exists = CatalogTaskMapper.selectById(taskDO.getId());
            if (exists != null) {
                if (taskDO.getProjectId() == null) {
                    taskDO.setProjectId(exists.getProjectId());
                }
                if (StringUtils.isEmpty(taskDO.getProjectCode())) {
                    taskDO.setProjectCode(exists.getProjectCode());
                }
            }
        }

        if (taskDO.getProjectId() != null) {
            String projectCode = taxonomyProjectApi.getProjectCodeByProjectId(taskDO.getProjectId());
            if (StringUtils.isNotEmpty(projectCode)) {
                taskDO.setProjectCode(projectCode);
            }
        } else if (StringUtils.isNotEmpty(taskDO.getProjectCode())) {
            taskDO.setProjectId(taxonomyProjectApi.getProjectIdByProjectCode(taskDO.getProjectCode()));
        }
    }

    private String resolveProjectCode(CatalogTaskDO taskDO) {
        if (taskDO == null) {
            throw new ServiceException("采集任务不存在");
        }
        fillProjectRelation(taskDO);
        return resolveProjectCode(taskDO.getProjectId(), taskDO.getProjectCode());
    }

    private String resolveProjectCode(CatalogTaskDO taskDO, CatalogTaskSchedulerDO scheduler) {
        if (taskDO != null) {
            fillProjectRelation(taskDO);
            if (taskDO.getProjectId() != null || StringUtils.isNotEmpty(taskDO.getProjectCode())) {
                return resolveProjectCode(taskDO.getProjectId(), taskDO.getProjectCode());
            }
        }
        return resolveProjectCode(scheduler);
    }

    private String resolveProjectCode(CatalogTaskRespVO task, CatalogTaskSchedulerDO scheduler) {
        if (task != null && (task.getProjectId() != null || StringUtils.isNotEmpty(task.getProjectCode()))) {
            return resolveProjectCode(task.getProjectId(), task.getProjectCode());
        }
        return resolveProjectCode(scheduler);
    }

    private String resolveProjectCode(CatalogTaskSchedulerDO scheduler) {
        if (scheduler == null) {
            throw new ServiceException("采集任务调度信息不存在");
        }
        return resolveProjectCode(scheduler.getProjectId(), scheduler.getProjectCode());
    }

    private String resolveProjectCode(Long projectId, String projectCode) {
        if (projectId != null) {
            String code = taxonomyProjectApi.getProjectCodeByProjectId(projectId);
            if (StringUtils.isNotEmpty(code)) {
                return code;
            }
        }
        if (StringUtils.isNotEmpty(projectCode)) {
            return projectCode;
        }
        throw new ServiceException("采集任务未关联项目或关联的项目编码不存在");
    }

    /**
     * 校验任务是否重复
     *
     * @param reqVO         任务信息
     * @param excludeTaskId 排除的任务ID(用于更新时排除自身)
     */
    private void validateDuplicateTask(CatalogTaskSaveReqVO reqVO, Long excludeTaskId) {
        String collectionScope = reqVO.getCollectionScope();
        Long datasourceId = reqVO.getDatasourceId();

        if (datasourceId == null) {
            return;
        }

        // 全部库 (0-全部库)
        if (CollectionScopeEnum.isAll(collectionScope)) {
            // 检查该数据源是否已有任何任务
            boolean exists = CatalogTaskMapper.existsByDatasourceId(datasourceId, excludeTaskId);
            if (exists) {
                throw new ServiceException("该数据源已被其他任务使用，无法重复添加", HttpStatus.CONFLICT);
            }
        }
        // 自定义库 (1-自定义库)
        else if (CollectionScopeEnum.isCustom(collectionScope)) {
            // 1. 检查是否存在全部库模式且相同数据源
            boolean hasAllScope = CatalogTaskMapper.existsByDatasourceAndScope(datasourceId, CollectionScopeEnum.ALL.getScope(), excludeTaskId);
            if (hasAllScope) {
                throw new ServiceException("该数据源已被全量采集任务使用，无法创建增量任务", HttpStatus.CONFLICT);
            }

            // 2. 检查是否有相同数据源的自定义库任务，且采集范围有重复的库
            List<CatalogTaskDO> existCustomTasks = CatalogTaskMapper.selectByDatasourceAndScope(datasourceId, CollectionScopeEnum.CUSTOM.getScope(), excludeTaskId);
            if (!CollectionUtils.isEmpty(existCustomTasks)) {
                // 获取当前任务的采集范围
                List<CatalogTaskScopeSaveReqVO> currentScopes = reqVO.getScopeSaveReqVOS();
                if (CollectionUtils.isEmpty(currentScopes)) {
                    return;
                }

                for (CatalogTaskDO existTask : existCustomTasks) {
                    // 获取已存在任务的采集范围
                    List<CatalogTaskScopeDO> existScopes = CatalogTaskScopeService.getCatalogTaskScopeListBytaskId(existTask.getId());
                    if (CollectionUtils.isEmpty(existScopes)) {
                        continue;
                    }

                    // 检查是否有重复的库
                    for (CatalogTaskScopeSaveReqVO currentScope : currentScopes) {
                        for (CatalogTaskScopeDO existScope : existScopes) {
                            if (isSameDatabase(currentScope, existScope)) {
                                String dbName = currentScope.getDbName();
                                String schemaName = currentScope.getSchemaName();
                                String dbInfo = StringUtils.isNotBlank(schemaName)
                                        ? dbName + "." + schemaName
                                        : dbName;
                                throw new ServiceException("采集范围中的数据库 [" + dbInfo + "] 已被其他任务使用", HttpStatus.CONFLICT);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 判断两个采集范围是否指向同一个数据库
     *
     * @param scope1 采集范围1
     * @param scope2 采集范围2
     * @return 是否相同
     */
    private boolean isSameDatabase(CatalogTaskScopeSaveReqVO scope1, CatalogTaskScopeDO scope2) {
        if (scope1 == null || scope2 == null) {
            return false;
        }
        return Objects.equals(scope1.getDbName(), scope2.getDbName()) &&
                Objects.equals(scope1.getSchemaName(), scope2.getSchemaName());
    }

    @Override
    public List<CatalogTaskSourceTreeRespVO> getSourceSystemTree() {
        // 1. 获取所有有效的来源系统
        List<TaxonomySourceSystemRespDTO> validSourceSystems = attSourceSystemApiService.getValidSourceSystems();
        if (CollectionUtils.isEmpty(validSourceSystems)) {
            return Lists.newArrayList();
        }

        // 2. 查询所有任务，用于构建数据源和数据库节点
        List<CatalogTaskDO> allTasks = CatalogTaskMapper.selectList();
        Map<Long, List<CatalogTaskDO>> tasksBySourceSystemMap = Maps.newHashMap();
        List<AssetsDatasourceRespDTO> daDatasourceRespDTOList = Lists.newArrayList();
        if (CollectionUtils.isNotEmpty(allTasks)) {
            // 按来源系统ID分组任务
            tasksBySourceSystemMap = allTasks.stream()
                    .filter(task -> task.getSourceSystemId() != null)
                    .collect(Collectors.groupingBy(CatalogTaskDO::getSourceSystemId));
            // 3. 获取所有任务涉及的数据源信息
            Set<Long> datasourceIds = allTasks.stream()
                    .map(CatalogTaskDO::getDatasourceId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            daDatasourceRespDTOList = daDatasourceApiService
                    .getDatabaseListByIds(new ArrayList<>(datasourceIds));
        }

        Map<Long, AssetsDatasourceRespDTO> daDatasourceRespDTOMap = daDatasourceRespDTOList
                .stream()
                .collect(Collectors.toMap(AssetsDatasourceRespDTO::getId, daDatasourceRespDTO -> daDatasourceRespDTO));

        List<CatalogTaskSourceTreeRespVO> treeList = Lists.newArrayList();

        // 4. 构建一级节点：来源系统
        for (TaxonomySourceSystemRespDTO sourceSystem : validSourceSystems) {
            CatalogTaskSourceTreeRespVO sourceNode = new CatalogTaskSourceTreeRespVO();
            sourceNode.setId(sourceSystem.getId());
            sourceNode.setName(sourceSystem.getName());
            sourceNode.setType("SOURCE");

            // 5. 获取该来源系统下的所有任务
            List<CatalogTaskDO> sourceSystemTasks = tasksBySourceSystemMap.getOrDefault(sourceSystem.getId(), Lists.newArrayList());

            // 6. 构建二级节点：数据源（去重）
            Map<Long, List<CatalogTaskDO>> datasourceGroupMap = sourceSystemTasks.stream()
                    .filter(task -> task.getDatasourceId() != null)
                    .collect(Collectors.groupingBy(CatalogTaskDO::getDatasourceId));

            List<CatalogTaskSourceTreeRespVO> datasourceChildren = Lists.newArrayList();
            for (Map.Entry<Long, List<CatalogTaskDO>> dsEntry : datasourceGroupMap.entrySet()) {
                Long datasourceId = dsEntry.getKey();
                List<CatalogTaskDO> dsTasks = dsEntry.getValue();
                if (CollectionUtils.isEmpty(dsTasks)) {
                    continue;
                }

                AssetsDatasourceRespDTO daDatasourceRespDTO = daDatasourceRespDTOMap.get(datasourceId);

                CatalogTaskSourceTreeRespVO datasourceNode = new CatalogTaskSourceTreeRespVO();
                datasourceNode.setId(datasourceId);
                datasourceNode.setName(daDatasourceRespDTO == null ? "" : daDatasourceRespDTO.getDatasourceName());
                datasourceNode.setDatasourceType(daDatasourceRespDTO == null ? "" : daDatasourceRespDTO.getDatasourceType());
                datasourceNode.setType("DATASOURCE");

                // 7. 构建三级节点：数据库（合并所有任务的数据库）
                List<CatalogTaskSourceTreeRespVO> dbChildren = Lists.newArrayList();
                for (CatalogTaskDO task : dsTasks) {
                    List<CatalogTaskSourceTreeRespVO> taskDbNodes = buildDatabaseNodes(task);
                    dbChildren.addAll(taskDbNodes);
                }
                datasourceNode.setChildren(dbChildren);

                datasourceChildren.add(datasourceNode);
            }

            sourceNode.setChildren(datasourceChildren);
            treeList.add(sourceNode);
        }

        return treeList;
    }

    /**
     * 构建数据库节点列表
     *
     * @param task 采集任务
     * @return 数据库节点列表
     */
    private List<CatalogTaskSourceTreeRespVO> buildDatabaseNodes(CatalogTaskDO task) {
        List<CatalogTaskSourceTreeRespVO> dbNodes = Lists.newArrayList();

        // 采集范围: 2-全部库, 1-自定义库
        if (CollectionScopeEnum.isAll(task.getCollectionScope())) {
            // 全部库：查询该数据源下的所有数据库
            List<CatalogDbDO> allDbs = CatalogDbMapper.selectList(
                    new QueryWrapper<CatalogDbDO>()
                            .eq("DATASOURCE_ID", task.getDatasourceId())
                            .eq("DEL_FLAG", "0")
            );

            if (CollectionUtils.isNotEmpty(allDbs)) {
                for (CatalogDbDO db : allDbs) {
                    CatalogTaskSourceTreeRespVO dbNode = new CatalogTaskSourceTreeRespVO();
                    dbNode.setId(db.getId());
                    dbNode.setName(db.getDbName());
                    dbNode.setType("DATABASE");
                    dbNode.setTaskId(task.getId());
                    dbNodes.add(dbNode);
                }
            }
        } else if (CollectionScopeEnum.isCustom(task.getCollectionScope())) {
            List<CatalogDbDO> allDbs = CatalogDbMapper.selectList(
                    new QueryWrapper<CatalogDbDO>()
                            .eq("DATASOURCE_ID", task.getDatasourceId())
                            .eq("DEL_FLAG", "0")
            );

            if (CollectionUtils.isNotEmpty(allDbs)) {
                for (CatalogDbDO db : allDbs) {
                    CatalogTaskSourceTreeRespVO dbNode = new CatalogTaskSourceTreeRespVO();
                    dbNode.setId(db.getId());
                    dbNode.setName(db.getDbName());
                    dbNode.setType("DATABASE");
                    dbNode.setTaskId(task.getId());
                    dbNodes.add(dbNode);
                }
            }
            // 自定义库：查询该任务的采集范围
            //List<CatalogTaskScopeDO> scopes = CatalogTaskScopeService.getCatalogTaskScopeListBytaskId(task.getId());
            //
            //if (CollectionUtils.isNotEmpty(scopes)) {
            //    for (CatalogTaskScopeDO scope : scopes) {
            //        CatalogTaskSourceTreeRespVO dbNode = new CatalogTaskSourceTreeRespVO();
            //        dbNode.setId(scope.getId());
            //        dbNode.setName(StringUtils.isNotBlank(scope.getSchemaName())
            //                ? scope.getDbName() + "." + scope.getSchemaName()
            //                : scope.getDbName());
            //        dbNode.setType("DATABASE");
            //        dbNode.setTaskId(task.getId());
            //        dbNodes.add(dbNode);
            //    }
            //}
        }

        return dbNodes;
    }
}
