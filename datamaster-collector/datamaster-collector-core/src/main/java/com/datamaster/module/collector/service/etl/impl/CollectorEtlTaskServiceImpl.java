

package com.datamaster.module.collector.service.etl.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.datamaster.api.ds.api.base.DsStatusRespDTO;
import com.datamaster.api.ds.api.etl.*;
import com.datamaster.api.ds.api.etl.ds.ProcessDefinition;
import com.datamaster.api.ds.api.etl.ds.ProcessTaskRelation;
import com.datamaster.api.ds.api.etl.ds.Schedule;
import com.datamaster.api.ds.api.etl.ds.TaskDefinition;
import com.datamaster.api.ds.api.service.etl.IDsEtlNodeService;
import com.datamaster.api.ds.api.service.etl.IDsEtlExecutorService;
import com.datamaster.api.ds.api.service.etl.IDsEtlSchedulerService;
import com.datamaster.api.ds.api.service.etl.IDsEtlTaskService;
import com.datamaster.common.core.domain.AjaxResult;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.database.core.DbColumn;
import com.datamaster.common.datasource.mgmt.api.IDatasourceApiService;
import com.datamaster.common.enums.CatType;
import com.datamaster.common.enums.ExecuteType;
import com.datamaster.common.enums.TaskCatEnum;
import com.datamaster.common.enums.TaskComponentTypeEnum;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.JSONUtils;
import com.datamaster.common.utils.SecurityUtils;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.common.utils.uuid.IdUtils;
import com.datamaster.common.category.dal.dataobject.TaxonomyCategoryDO;
import com.datamaster.common.category.dal.mapper.TaxonomyCategoryMapper;
import com.datamaster.common.category.service.ITaxonomyCategoryService;
import com.datamaster.common.api.space.ISpaceApi;
import com.datamaster.module.collector.api.etl.dto.CollectorEtlTaskRespDTO;
import com.datamaster.module.collector.api.etl.dto.CollectorEtlNodeRespDTO;
import com.datamaster.module.collector.api.etl.dto.CollectorEtlTaskNodeRelRespDTO;
import com.datamaster.module.collector.api.service.etl.CollectorEtlTaskService;
import com.datamaster.module.collector.controller.admin.etl.vo.*;
import com.datamaster.module.collector.dal.dataobject.etl.*;
import com.datamaster.module.collector.dal.mapper.etl.CollectorEtlTaskMapper;
import com.datamaster.module.collector.service.etl.*;
import com.datamaster.module.collector.utils.ComponentGraphCompiler;
import com.datamaster.module.collector.utils.IDGeneratorUtils;
import com.datamaster.flinkx.core.FlinkxEtlTaskConverter;
import com.datamaster.module.collector.utils.PublishedLineageAssembler;
import com.datamaster.module.collector.utils.TaskConverter;
import com.datamaster.module.collector.utils.SourceMetadataSupport;
import com.datamaster.module.collector.utils.model.DsResource;
import com.datamaster.module.collector.utils.model.FlinkxIncrementalConfig;
import com.datamaster.mybatis.config.MasterDataSourceConfig;
import com.datamaster.mybatis.core.util.MyBatisUtils;
import com.datamaster.redis.service.IRedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toSet;
import static com.datamaster.common.core.domain.AjaxResult.error;
import static com.datamaster.common.core.domain.AjaxResult.success;

/**
 * 数据集成任务Service业务层处理
 *
 * @author DATAMASTER
 * @date 2025-02-13
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class CollectorEtlTaskServiceImpl extends ServiceImpl<CollectorEtlTaskMapper, CollectorEtlTaskDO> implements ICollectorEtlTaskService, CollectorEtlTaskService {
    @Resource
    private CollectorEtlTaskMapper CollectorEtlTaskMapper;

    @Resource
    private ICollectorEtlSchedulerService iCollectorEtlSchedulerService;

    @Resource
    private IDsEtlSchedulerService iDsEtlSchedulerService;

    @Resource
    private IDsEtlNodeService dsEtlNodeService;
    @Resource
    private IDsEtlTaskService dsEtlTaskService;
    @Resource
    private IDsEtlExecutorService dsEtlExecutorService;
    @Resource
    private ICollectorEtlNodeService iCollectorEtlNodeService;

    @Resource
    private ICollectorEtlTaskLogService iCollectorEtlTaskLogService;
    @Resource
    private ICollectorEtlNodeLogService iCollectorEtlNodeLogService;
    @Resource
    private ICollectorEtlTaskNodeRelService iCollectorEtlTaskNodeRelService;
    @Resource
    private ICollectorEtlTaskNodeRelLogService iCollectorEtlTaskNodeRelLogService;
    @Resource
    private ICollectorEtlTaskInstanceService CollectorEtlTaskInstanceService;
    @Resource
    private ICollectorEtlIncrementalService collectorEtlIncrementalService;
    @Resource
    private ITaxonomyCategoryService taxonomyCategoryService;
    @Resource
    private TaxonomyCategoryMapper taxonomyCategoryMapper;
    @Resource
    private ICollectorEtlTaskExtService CollectorEtlTaskExtService;
    @Resource
    private IRedisService redisService;
    @Resource
    private ISpaceApi TaxonomySpaceApi;

    /** 用于发布前确认目标表已经包含逻辑集成节点要求的治理列。 */
    @Resource
    private IDatasourceApiService datasourceApiService;
    @Resource private com.datamaster.module.collector.dal.mapper.etl.CollectorEtlRunMapper runGuardMapper;

    @Value("${ds.run_guard.enabled:false}")
    private boolean runGuardEnabled;
    @Value("${ds.run_guard.prepare_url:}")
    private String runGuardPrepareUrl;
    @Value("${ds.incremental_fixed_window.enabled:false}") private boolean stageIncrementalEnabled;
    @Value("${ds.stage_prepare_url:}") private String stagePrepareUrl;

    @Value("${ds.incremental_prepare_url:http://127.0.0.1:8080/col/etlTask/incremental/prepare}")
    private String incrementalPrepareUrl;
    @Value("${ds.incremental_complete_url:http://127.0.0.1:8080/col/etlTask/incremental/complete}")
    private String incrementalCompleteUrl;
    @Value("${ds.collector_url:http://127.0.0.1:8080/cat/taskExecutor/runExecuteTask}")
    private String catalogTaskExecuteUrl;
    @Value("${ds.quality_url:http://127.0.0.1:8080/quality/qualityTaskExecutor/runExecuteTask}")
    private String qualityTaskExecuteUrl;

    @Resource
    private CollectorLineageSyncService collectorLineageSyncService;

    @Override
    public PageResult<CollectorEtlTaskDO> getCollectorEtlTaskPage(CollectorEtlTaskPageReqVO pageReqVO) {
        return CollectorEtlTaskMapper.selectPage(pageReqVO);
    }

    public List<CollectorEtlTaskNodeRelRespVO> getTaskNodeRelList(CollectorEtlTaskRespVO bean) {
        if (bean == null || bean.getId() == null) {
            return new ArrayList<>();
        }
        CollectorEtlTaskNodeRelPageReqVO reqVO = new CollectorEtlTaskNodeRelPageReqVO();
        reqVO.setTaskId(bean.getId());
        reqVO.setTaskCode(bean.getCode());
        reqVO.setTaskVersion(bean.getVersion());
        List<CollectorEtlTaskNodeRelRespVO> result = iCollectorEtlTaskNodeRelService.getCollectorEtlTaskNodeRelRespVOList(reqVO);
        return result == null ? new ArrayList<>() : result;
    }

    @Resource private com.datamaster.module.collector.dal.mapper.etl.CollectorEtlTaskInstanceMapper runtimeInstanceMapper;

    @Override
    public PageResult<CollectorEtlTaskRespVO> getCollectorEtlTaskPageList(CollectorEtlTaskPageReqVO CollectorEtlTask) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) throw new ServiceException("请登录后查询任务");
        if (!SecurityUtils.isAdmin(userId)) {
            if ((CollectorEtlTask.getSpaceId() == null && StringUtils.isBlank(CollectorEtlTask.getSpaceCode()))
                    || !TaxonomySpaceApi.hasUserSpace(userId, CollectorEtlTask.getSpaceId(), CollectorEtlTask.getSpaceCode())) {
                throw new ServiceException("无权查询该空间任务", 403);
            }
        }
        IPage<CollectorEtlTaskRespVO> mpPage = CollectorEtlTaskMapper.getCollectorEtlTaskPage(MyBatisUtils.buildPage(CollectorEtlTask), CollectorEtlTask);//BeanUtils.toBean(CollectorEtlTaskDOPageResult, CollectorEtlTaskRespVO.class);
        fillComponentLatestRuns(mpPage.getRecords());
        return new PageResult(mpPage.getRecords(), mpPage.getTotal());
    }

    private void fillComponentLatestRuns(List<CollectorEtlTaskRespVO> rows) {
        if (rows == null || rows.isEmpty()) return;
        Set<Long> ids = rows.stream().map(CollectorEtlTaskRespVO::getId).collect(Collectors.toSet());
        Set<Long> components = new HashSet<>();
        for (CollectorEtlTaskExtDO ext : CollectorEtlTaskExtService.list(Wrappers.lambdaQuery(CollectorEtlTaskExtDO.class)
                .in(CollectorEtlTaskExtDO::getTaskId, ids))) {
            if (StringUtils.isBlank(ext.getFlinkxJobTemplateJson())) continue;
            JSONObject snapshot = JSON.parseObject(ext.getFlinkxJobTemplateJson());
            if (CollectorEtlStageRuntimeService.KIND.equals(snapshot.getString("kind"))) components.add(ext.getTaskId());
        }
        if (components.isEmpty()) return;
        Map<Long, CollectorEtlTaskInstanceDO> latest = new HashMap<>();
        for (CollectorEtlTaskInstanceDO instance : runtimeInstanceMapper.latestMainInstances(components)) {
            latest.put(instance.getTaskId(), instance);
        }
        for (CollectorEtlTaskRespVO row : rows) {
            if (!components.contains(row.getId())) continue;
            CollectorEtlTaskInstanceDO instance = latest.get(row.getId());
            row.setLastExecuteDsId(instance == null ? null : String.valueOf(instance.getDsId()));
            row.setLastExecuteStatus(instance == null ? null : instance.getStatus());
            row.setLastExecuteTime(instance == null ? null : instance.getEndTime() != null ? instance.getEndTime()
                    : instance.getStartTime() != null ? instance.getStartTime() : instance.getCreateTime());
        }
    }

    /** 以任务持久化归属校验当前用户空间权限，避免客户端 spaceCode 绕过。 */
    private void checkTaskSpaceAccess(CollectorEtlTaskDO task) {
        if (task == null) return;
        Long userId = SecurityUtils.getUserId();
        if (userId == null) throw new ServiceException("请登录后访问数据集成任务");
        if (SecurityUtils.isAdmin(userId)) return;
        if (!TaxonomySpaceApi.hasUserSpace(userId, task.getSpaceId(), task.getSpaceCode())) {
            throw new ServiceException("无权访问或操作该空间的数据集成任务");
        }
    }

    @Override
    public Long createCollectorEtlTask(CollectorEtlTaskSaveReqVO createReqVO) {
        CollectorEtlTaskDO CollectorEtlTaskDO = BeanUtils.toBean(createReqVO, CollectorEtlTaskDO.class);
        if (StringUtils.isNotEmpty(CollectorEtlTaskDO.getCatCode()) && StringUtils.isNotEmpty(createReqVO.getType())) {
            TaskCatEnum taskCatEnum = TaskCatEnum.findEnumByType(createReqVO.getType());
            if (taskCatEnum != null) {
                CollectorEtlTaskDO.setCatId(taxonomyCategoryMapper.getCatIdByCatTypeAndCode(taskCatEnum.getCatType().getValue(), CollectorEtlTaskDO.getCatCode()));
            }
        }
        CollectorEtlTaskMapper.insert(CollectorEtlTaskDO);
        return CollectorEtlTaskDO.getId();
    }

    @Override
    public int updateCollectorEtlTask(CollectorEtlTaskSaveReqVO updateReqVO) {
        // 相关校验

        // 更新数据集成任务
        CollectorEtlTaskDO updateObj = BeanUtils.toBean(updateReqVO, CollectorEtlTaskDO.class);
        if (StringUtils.isNotEmpty(updateReqVO.getCatCode()) && StringUtils.isNotEmpty(updateReqVO.getType())) {
            TaskCatEnum taskCatEnum = TaskCatEnum.findEnumByType(updateReqVO.getType());
            if (taskCatEnum != null) {
                updateReqVO.setCatId(taxonomyCategoryMapper.getCatIdByCatTypeAndCode(taskCatEnum.getCatType().getValue(), updateReqVO.getCatCode()));
            }
        }
        return CollectorEtlTaskMapper.updateById(updateObj);
    }

    @Override
    public int removeCollectorEtlTask(Collection<Long> idList) {
        // 批量删除前先校验全部任务归属，避免部分删除后其余任务因权限失败。
        for (Long id : new TreeSet<>(idList)) {
            checkTaskSpaceAccess(CollectorEtlTaskMapper.selectById(id));
            CollectorEtlTaskExtDO ext = CollectorEtlTaskExtService.getByTaskId(id);
            if (runGuardEnabled && isComponentSnapshot(ext)) {
                runGuardMapper.lockTask(id);
                if (!runGuardMapper.findActive(id).isEmpty()) throw new ServiceException("组件任务仍有活动或待核对运行，确认结束前不能删除");
            }
        }
        int sum = 0;
        for (Long id : idList) {
            CollectorEtlTaskDO CollectorEtlTaskDO = CollectorEtlTaskMapper.selectById(id);
            if (CollectorEtlTaskDO == null) {
                sum++;
                continue;
            }
            if (StringUtils.equals("1", CollectorEtlTaskDO.getStatus())) {
                throw new ServiceException("上线任务，不允删除，请先下线！");
            }

            //1：离线任务 2：实时任务 3：数据开发任务 4：作业任务
            String type = CollectorEtlTaskDO.getType();
            CollectorEtlTaskExtDO taskExt = StringUtils.equals("1", type)
                    ? CollectorEtlTaskExtService.getByTaskId(CollectorEtlTaskDO.getId()) : null;
            String dsTaskCode = CollectorEtlTaskDO.getCode();
            boolean hasDsTask = CollectorEtlTaskDO.getDsId() != null && CollectorEtlTaskDO.getDsId() > 0;
            if (taskExt != null && StringUtils.isNotEmpty(taskExt.getEtlTaskCode())) {
                dsTaskCode = taskExt.getEtlTaskCode();
                hasDsTask = true;
            }
            if (hasDsTask) {
                dsEtlTaskService.deleteTask(CollectorEtlTaskDO.getSpaceCode(), dsTaskCode);
            }
            collectorLineageSyncService.lifecycle(CollectorEtlTaskDO, "DELETE");
            sum += CollectorEtlTaskMapper.deleteById(id);
        }
        // 批量删除数据集成任务
        return sum;
    }

    public List<CollectorEtlNodeRespVO> removeDuplicateById(List<CollectorEtlNodeRespVO> etlNodeLogRespVOList, String type) {
        // 使用 LinkedHashMap 保证去重后保持原顺序
        Map<Long, CollectorEtlNodeRespVO> map = etlNodeLogRespVOList.stream()
                .filter(itam -> itam != null && itam.getId() != null)
                .collect(Collectors.toMap(CollectorEtlNodeRespVO::getId, vo -> vo, (existing, replacement) -> existing));

        // 获取去重后的 List
        ArrayList<CollectorEtlNodeRespVO> CollectorEtlNodeRespVOS = new ArrayList<>(map.values());
        if (StringUtils.equals("4", type) && CollectionUtils.isNotEmpty(CollectorEtlNodeRespVOS)) {
            for (CollectorEtlNodeRespVO CollectorEtlNodeRespVO : CollectorEtlNodeRespVOS) {
                String parameters = CollectorEtlNodeRespVO.getParameters();
                Map<String, Object> stringObjectMap = JSONUtils.convertTaskDefinitionJsonMap(parameters);
                long subTaskId = MapUtils.getLongValue(stringObjectMap, "subTaskId");
                CollectorEtlTaskDO CollectorEtlTaskDO = CollectorEtlTaskMapper.selectById(subTaskId);
                CollectorEtlNodeRespVO.setReleaseState(CollectorEtlTaskDO.getStatus());
            }
        }
        return CollectorEtlNodeRespVOS;
    }

    @Override
    public List<CollectorEtlTaskDO> getCollectorEtlTaskList() {
        return CollectorEtlTaskMapper.selectList();
    }

    @Override
    public Map<Long, CollectorEtlTaskDO> getCollectorEtlTaskMap() {
        List<CollectorEtlTaskDO> CollectorEtlTaskList = CollectorEtlTaskMapper.selectList();
        return CollectorEtlTaskList.stream()
                .collect(Collectors.toMap(
                        CollectorEtlTaskDO::getId,
                        CollectorEtlTaskDO -> CollectorEtlTaskDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing
                ));
    }


    /**
     * 导入数据集成任务数据
     *
     * @param importExcelList 数据集成任务数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName        操作用户
     * @return 结果
     */
    @Override
    public String importCollectorEtlTask(List<CollectorEtlTaskRespVO> importExcelList, boolean isUpdateSupport, String operName) {
        if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
            throw new ServiceException("导入数据不能为空！");
        }

        int successNum = 0;
        int failureNum = 0;
        List<String> successMessages = new ArrayList<>();
        List<String> failureMessages = new ArrayList<>();

        for (CollectorEtlTaskRespVO respVO : importExcelList) {
            try {
                CollectorEtlTaskDO CollectorEtlTaskDO = BeanUtils.toBean(respVO, CollectorEtlTaskDO.class);
                Long CollectorEtlTaskId = respVO.getId();
                if (isUpdateSupport) {
                    if (CollectorEtlTaskId != null) {
                        CollectorEtlTaskDO existingCollectorEtlTask = CollectorEtlTaskMapper.selectById(CollectorEtlTaskId);
                        if (existingCollectorEtlTask != null) {
                            CollectorEtlTaskMapper.updateById(CollectorEtlTaskDO);
                            successNum++;
                            successMessages.add("数据更新成功，ID为 " + CollectorEtlTaskId + " 的数据集成任务记录。");
                        } else {
                            failureNum++;
                            failureMessages.add("数据更新失败，ID为 " + CollectorEtlTaskId + " 的数据集成任务记录不存在。");
                        }
                    } else {
                        failureNum++;
                        failureMessages.add("数据更新失败，某条记录的ID不存在。");
                    }
                } else {
                    QueryWrapper<CollectorEtlTaskDO> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("id", CollectorEtlTaskId);
                    CollectorEtlTaskDO existingCollectorEtlTask = CollectorEtlTaskMapper.selectOne(queryWrapper);
                    if (existingCollectorEtlTask == null) {
                        CollectorEtlTaskMapper.insert(CollectorEtlTaskDO);
                        successNum++;
                        successMessages.add("数据插入成功，ID为 " + CollectorEtlTaskId + " 的数据集成任务记录。");
                    } else {
                        failureNum++;
                        failureMessages.add("数据插入失败，ID为 " + CollectorEtlTaskId + " 的数据集成任务记录已存在。");
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
    public Long getNodeUniqueKey(CollectorEtlNewNodeSaveReqVO CollectorEtlNewNodeSaveReqVO) {
        DsNodeGenCodeRespDTO dsNodeGenCodeRespDTO = dsEtlNodeService.genCode(CollectorEtlNewNodeSaveReqVO.getSpaceCode());
        return dsNodeGenCodeRespDTO.getData().get(0);
    }

    @Override
    public Long getLocalNodeUniqueKey() {
        return IDGeneratorUtils.getLongId();
    }

    @Override
    public List<CollectorEtlTaskRespVO> getSubTaskStatusList(CollectorEtlTaskPageReqVO CollectorEtlTask) {
        CollectorEtlTaskRespVO CollectorEtlTaskById = this.getCollectorEtlTaskById(CollectorEtlTask.getId());
        List<CollectorEtlNodeRespVO> taskDefinitionList = CollectorEtlTaskById.getTaskDefinitionList();

        List<CollectorEtlTaskRespVO> CollectorEtlNewNodeSaveReqVOS = new ArrayList<>();
        //循环获取自定义任务
        for (CollectorEtlNodeRespVO CollectorEtlNodeRespVO : taskDefinitionList) {
            String parameters = CollectorEtlNodeRespVO.getParameters();
            Map<String, Object> stringObjectMap = JSONUtils.convertTaskDefinitionJsonMap(parameters);
            long subTaskId = MapUtils.getLongValue(stringObjectMap, "subTaskId");
            CollectorEtlTaskDO CollectorEtlTaskDO = CollectorEtlTaskMapper.selectById(subTaskId);
            if (CollectorEtlTaskDO != null) {
                CollectorEtlNewNodeSaveReqVOS.add(BeanUtils.toBean(CollectorEtlTaskDO, CollectorEtlTaskRespVO.class));
            }
        }
        return CollectorEtlNewNodeSaveReqVOS;
    }

    @Override
    public Map<String, Object> updateReleaseJobTask(CollectorEtlNewNodeSaveReqVO CollectorEtlNewNodeSaveReqVO) {
        CollectorEtlTaskDO CollectorEtlTaskDO = CollectorEtlTaskMapper.selectById(CollectorEtlNewNodeSaveReqVO.getIdStr());
        checkTaskSpaceAccess(CollectorEtlTaskDO);
        CollectorEtlSchedulerPageReqVO CollectorEtlSchedulerPageReqVO = new CollectorEtlSchedulerPageReqVO();
        CollectorEtlSchedulerPageReqVO.setTaskId(CollectorEtlTaskDO.getId());
        CollectorEtlSchedulerPageReqVO.setTaskCode(CollectorEtlTaskDO.getCode());
        //1：离线任务 2：实时任务 3：数据开发任务 4：作业任务
        String type = CollectorEtlTaskDO.getType();
        CollectorEtlSchedulerDO CollectorEtlSchedulerById = iCollectorEtlSchedulerService.getCollectorEtlSchedulerById(CollectorEtlSchedulerPageReqVO);
        // 若任务状态未变化，则直接返回
        if (StringUtils.equals(CollectorEtlTaskDO.getStatus(), CollectorEtlNewNodeSaveReqVO.getReleaseState())) {
            return new HashMap<>();
        }

        if (StringUtils.equals("-2", CollectorEtlTaskDO.getStatus()) && StringUtils.equals("0", CollectorEtlNewNodeSaveReqVO.getReleaseState())) {
            return new HashMap<>();
        }

        if (StringUtils.equals("-3", CollectorEtlTaskDO.getStatus()) && StringUtils.equals("1", CollectorEtlNewNodeSaveReqVO.getReleaseState())) {
            return new HashMap<>();
        }


        if (StringUtils.equals("4", type) && StringUtils.equals("1", CollectorEtlNewNodeSaveReqVO.getReleaseState())) {
            wrapCustomNodeStatus(CollectorEtlTaskDO.getId(), CollectorEtlNewNodeSaveReqVO.getReleaseState());
        }

        if (StringUtils.equals("1", CollectorEtlSchedulerById.getStatus()) && StringUtils.equals("0", CollectorEtlNewNodeSaveReqVO.getReleaseState())) {
            throw new ServiceException("调度上线中，请先下线调度！");
        }

        CollectorEtlTaskDO.setCode(resolvePublishedDsTaskCode(CollectorEtlTaskDO));

        // 下线操作
        if (StringUtils.equals("0", CollectorEtlNewNodeSaveReqVO.getReleaseState())) {
            DsStatusRespDTO dsStatusRespDTO = dsEtlTaskService.releaseTask("OFFLINE", String.valueOf(CollectorEtlTaskDO.getSpaceCode()), CollectorEtlTaskDO.getCode());
            if (!isDsStatusSuccess(dsStatusRespDTO)) {
                throw new ServiceException("发布或下线任务，失败！");
            }

            // 更新任务状态
            if (!StringUtils.equals("-2", CollectorEtlTaskDO.getStatus()) && !StringUtils.equals("-3", CollectorEtlTaskDO.getStatus())) {
                updateTaskStatus(CollectorEtlTaskDO.getId(), CollectorEtlNewNodeSaveReqVO.getReleaseState());
            } else {
                updateTaskStatus(CollectorEtlTaskDO.getId(), "-2");
            }
            collectorLineageSyncService.lifecycle(CollectorEtlTaskDO, "INACTIVE");
            return new HashMap<>();
        }

        // 上线操作
        DsStatusRespDTO dsStatusRespDTO = dsEtlTaskService.releaseTask("ONLINE", String.valueOf(CollectorEtlTaskDO.getSpaceCode()), CollectorEtlTaskDO.getCode());
        String responseMsg = dsStatusRespDTO == null ? null : dsStatusRespDTO.getMsg();
        if (responseMsg != null && responseMsg.contains("SubWorkflowDefinition") && responseMsg.contains("is not online")) {
            throw new RuntimeException("存在未上线的子工作流，请先将所有子工作流上线");
        }
        if (!isDsStatusSuccess(dsStatusRespDTO)) {
            throw new ServiceException("发布任务失败！");
        }

        // 更新任务状态
        if (!StringUtils.equals("-2", CollectorEtlTaskDO.getStatus()) && !StringUtils.equals("-3", CollectorEtlTaskDO.getStatus())) {
            updateTaskStatus(CollectorEtlTaskDO.getId(), CollectorEtlNewNodeSaveReqVO.getReleaseState());
        } else {
            updateTaskStatus(CollectorEtlTaskDO.getId(), "-3");
        }
        collectorLineageSyncService.lifecycle(CollectorEtlTaskDO, "ACTIVE");

        return null;
    }

    @Override
    public Map<String, Object> updateReleaseSchedule(CollectorEtlNewNodeSaveReqVO CollectorEtlNewNodeSaveReqVO) {
        CollectorEtlTaskDO CollectorEtlTaskDO = CollectorEtlTaskMapper.selectById(CollectorEtlNewNodeSaveReqVO.getIdStr());
        checkTaskSpaceAccess(CollectorEtlTaskDO);
        if (StringUtils.equals("1", CollectorEtlNewNodeSaveReqVO.getSchedulerState())
                && isStreamingFlinkxTask(CollectorEtlTaskDO.getId())) {
            throw new ServiceException("CDC/流式任务不支持定时调度，请直接发布任务运行");
        }
        CollectorEtlSchedulerDO CollectorEtlSchedulerById = getCollectorEtlScheduler(CollectorEtlTaskDO.getCode(), CollectorEtlTaskDO.getId());
        if (CollectorEtlSchedulerById == null || CollectorEtlSchedulerById.getId() == null) {
            throw new ServiceException("任务模版错误，未查询到调度信息！");
        }

        // 若任务状态未变化，则直接返回
        if (StringUtils.equals(CollectorEtlSchedulerById.getStatus(), CollectorEtlNewNodeSaveReqVO.getSchedulerState())) {
            return new HashMap<>();
        }

        if ((StringUtils.equals("0", CollectorEtlTaskDO.getStatus()) || StringUtils.equals("-2", CollectorEtlTaskDO.getStatus()))
                && StringUtils.equals("1", CollectorEtlNewNodeSaveReqVO.getSchedulerState())) {
            throw new ServiceException("任务未上线，请先上线！");
        }

        //1：离线任务 2：实时任务 3：数据开发任务 4：作业任务
        String type = CollectorEtlTaskDO.getType();

        //判断是否是离线任务 是需要获取扩展信息的任务编码进行接口调用
        if (StringUtils.equals("1", type)) {
            //获取扩展信息
            CollectorEtlTaskExtDO taskExt = CollectorEtlTaskExtService.getByTaskId(Long.parseLong(CollectorEtlNewNodeSaveReqVO.getIdStr()));
            if (taskExt == null) {
                throw new ServiceException("暂无数据！");
            }
            CollectorEtlTaskDO.setCode(taskExt.getEtlTaskCode());
        }

        if (StringUtils.equals("4", type) && StringUtils.equals("1", CollectorEtlNewNodeSaveReqVO.getSchedulerState())) {
            wrapCustomNodeStatus(CollectorEtlTaskDO.getId(), "1");
        }

        // 下线操作
        if (StringUtils.equals("0", CollectorEtlNewNodeSaveReqVO.getSchedulerState())) {
            if (CollectorEtlSchedulerById.getDsId() != null && CollectorEtlSchedulerById.getDsId() > 0) {
                DsStatusRespDTO dsStatusRespDTO1 = iDsEtlSchedulerService.offlineScheduler(CollectorEtlTaskDO.getSpaceCode(), CollectorEtlSchedulerById.getDsId());
                if (!isDsStatusSuccess(dsStatusRespDTO1)) {
                    throw new ServiceException("下线调度器，失败！");
                }
            }

            // 更新调度器并上线
            CollectorEtlSchedulerSaveReqVO CollectorEtlSchedulerSaveReqVO = new CollectorEtlSchedulerSaveReqVO();
            CollectorEtlSchedulerSaveReqVO.setId(CollectorEtlSchedulerById.getId());
            CollectorEtlSchedulerSaveReqVO.setStatus(CollectorEtlNewNodeSaveReqVO.getSchedulerState());
            // 更新调度器
            iCollectorEtlSchedulerService.updateCollectorEtlScheduler(CollectorEtlSchedulerSaveReqVO);
            return null;
        }

        DsSchedulerRespDTO dsSchedulerRespDTO;
        if (CollectorEtlSchedulerById.getDsId() == null || CollectorEtlSchedulerById.getDsId() < 1) {
            dsSchedulerRespDTO = createOrUpdateScheduler(CollectorEtlSchedulerById, CollectorEtlTaskDO);
        } else {
            dsSchedulerRespDTO = updateExistingScheduler(CollectorEtlSchedulerById, CollectorEtlTaskDO);
        }

        // 更新调度器并上线
        CollectorEtlSchedulerSaveReqVO CollectorEtlSchedulerSaveReqVO = TaskConverter.convertToCollectorEtlSchedulerSaveReqVO(dsSchedulerRespDTO, CollectorEtlTaskDO);
        CollectorEtlSchedulerSaveReqVO.setId(CollectorEtlSchedulerById.getId());
        CollectorEtlSchedulerSaveReqVO.setStatus(CollectorEtlNewNodeSaveReqVO.getSchedulerState());

        DsStatusRespDTO dsStatusRespDTO1 = iDsEtlSchedulerService.onlineScheduler(CollectorEtlTaskDO.getSpaceCode(), CollectorEtlSchedulerSaveReqVO.getDsId());
        if (!isDsStatusSuccess(dsStatusRespDTO1)) {
            throw new ServiceException("上线调度器，失败！");
        }

        // 更新调度器
        iCollectorEtlSchedulerService.updateCollectorEtlScheduler(CollectorEtlSchedulerSaveReqVO);
        return null;
    }

    @Override
    public CollectorEtlTaskSaveReqVO updateEtlTask(CollectorEtlNewNodeSaveReqVO CollectorEtlNewNodeSaveReqVO) {
        CollectorEtlTaskDO CollectorEtlTaskDO = CollectorEtlTaskMapper.selectById(CollectorEtlNewNodeSaveReqVO.getIdStr());
        checkTaskSpaceAccess(CollectorEtlTaskDO);
        if (StringUtils.equals("1", CollectorEtlTaskDO.getStatus()) || StringUtils.equals("-3", CollectorEtlTaskDO.getStatus())) {
            throw new ServiceException("上线任务，不允许修改，请先下线！");
        }

        return saveEtlTaskLocal(CollectorEtlNewNodeSaveReqVO);
    }

    @Override
    public Map<String, Object> updateReleaseTask(CollectorEtlNewNodeSaveReqVO CollectorEtlNewNodeSaveReqVO) {
        CollectorEtlTaskDO CollectorEtlTaskDO = CollectorEtlTaskMapper.selectById(CollectorEtlNewNodeSaveReqVO.getIdStr());
        checkTaskSpaceAccess(CollectorEtlTaskDO);
        CollectorEtlSchedulerPageReqVO CollectorEtlSchedulerPageReqVO = new CollectorEtlSchedulerPageReqVO();
        CollectorEtlSchedulerPageReqVO.setTaskId(CollectorEtlTaskDO.getId());
        CollectorEtlSchedulerPageReqVO.setTaskCode(CollectorEtlTaskDO.getCode());
        CollectorEtlSchedulerDO CollectorEtlSchedulerById = iCollectorEtlSchedulerService.getCollectorEtlSchedulerById(CollectorEtlSchedulerPageReqVO);

        if (CollectorEtlSchedulerById == null) {
            throw new ServiceException("任务模版错误，未查询到调度信息！");
        }

        // 若任务状态未变化，则直接返回
        if (StringUtils.equals(CollectorEtlTaskDO.getStatus(), CollectorEtlNewNodeSaveReqVO.getReleaseState())) {
            return new HashMap<>();
        }

        if (StringUtils.equals("-2", CollectorEtlTaskDO.getStatus()) && StringUtils.equals("0", CollectorEtlNewNodeSaveReqVO.getReleaseState())) {
            return new HashMap<>();
        }

        if (StringUtils.equals("-3", CollectorEtlTaskDO.getStatus()) && StringUtils.equals("1", CollectorEtlNewNodeSaveReqVO.getReleaseState())) {
            return new HashMap<>();
        }

        //1：离线任务 2：实时任务 3：数据开发任务 4：作业任务
        String type = CollectorEtlTaskDO.getType();
        if (StringUtils.equals("4", type)) {
            wrapCustomNodeStatus(CollectorEtlTaskDO.getId(), CollectorEtlNewNodeSaveReqVO.getReleaseState());
        }

//        try{
        collectMainTaskIdsForStatusChange(CollectorEtlNewNodeSaveReqVO, CollectorEtlTaskDO, CollectorEtlSchedulerById);
//        }catch (Exception e){
//            if(StringUtils.equals("4",type)){
//                String releaseState = CollectorEtlNewNodeSaveReqVO.getReleaseState();
//                wrapCustomNodeStatus(CollectorEtlTaskDO.getId(),StringUtils.equals("1",releaseState) ? "0":"1");
//            }
//            throw e;
//        }
        return new HashMap<>();
    }

    private void collectMainTaskIdsForStatusChange(CollectorEtlNewNodeSaveReqVO CollectorEtlNewNodeSaveReqVO, CollectorEtlTaskDO CollectorEtlTaskDO, CollectorEtlSchedulerDO CollectorEtlSchedulerById) {

        // 下线操作
        if (StringUtils.equals("0", CollectorEtlNewNodeSaveReqVO.getReleaseState())) {
            if (CollectorEtlSchedulerById.getDsId() != null && CollectorEtlSchedulerById.getDsId() > 0) {
                DsStatusRespDTO dsStatusRespDTO = dsEtlTaskService.releaseTask("OFFLINE", String.valueOf(CollectorEtlTaskDO.getSpaceCode()), CollectorEtlTaskDO.getCode());
                if (!isDsStatusSuccess(dsStatusRespDTO)) {
                    throw new ServiceException("发布或下线任务，失败！");
                }
                DsStatusRespDTO dsStatusRespDTO1 = iDsEtlSchedulerService.offlineScheduler(CollectorEtlTaskDO.getSpaceCode(), CollectorEtlSchedulerById.getDsId());
                if (!isDsStatusSuccess(dsStatusRespDTO1)) {
                    throw new ServiceException("下线调度器，失败！");
                }
            }
            // 更新任务状态
            if (!StringUtils.equals("-2", CollectorEtlTaskDO.getStatus()) && !StringUtils.equals("-3", CollectorEtlTaskDO.getStatus())) {
                updateTaskStatus(CollectorEtlTaskDO.getId(), CollectorEtlNewNodeSaveReqVO.getReleaseState());
            } else {
                updateTaskStatus(CollectorEtlTaskDO.getId(), "-2");
            }
            collectorLineageSyncService.lifecycle(CollectorEtlTaskDO, "INACTIVE");
            return;
        }

        // 上线操作
        DsStatusRespDTO dsStatusRespDTO = dsEtlTaskService.releaseTask("ONLINE", String.valueOf(CollectorEtlTaskDO.getSpaceCode()), CollectorEtlTaskDO.getCode());
        String responseMsg = dsStatusRespDTO == null ? null : dsStatusRespDTO.getMsg();
        if (responseMsg != null && responseMsg.contains("SubWorkflowDefinition") && responseMsg.contains("is not online")) {
            throw new RuntimeException("存在未上线的子工作流，请先将所有子工作流上线");
        }
        if (!isDsStatusSuccess(dsStatusRespDTO)) {
            throw new ServiceException("发布任务失败！");
        }

        DsSchedulerRespDTO dsSchedulerRespDTO;
        if (CollectorEtlSchedulerById.getDsId() == null || CollectorEtlSchedulerById.getDsId() < 1) {
            dsSchedulerRespDTO = createOrUpdateScheduler(CollectorEtlSchedulerById, CollectorEtlTaskDO);
        } else {
            dsSchedulerRespDTO = updateExistingScheduler(CollectorEtlSchedulerById, CollectorEtlTaskDO);
        }

        // 更新调度器并上线
        CollectorEtlSchedulerSaveReqVO CollectorEtlSchedulerSaveReqVO = TaskConverter.convertToCollectorEtlSchedulerSaveReqVO(dsSchedulerRespDTO, CollectorEtlTaskDO);
        CollectorEtlSchedulerSaveReqVO.setId(CollectorEtlSchedulerById.getId());
        CollectorEtlSchedulerSaveReqVO.setStatus("1");

        DsStatusRespDTO dsStatusRespDTO1 = iDsEtlSchedulerService.onlineScheduler(CollectorEtlTaskDO.getSpaceCode(), CollectorEtlSchedulerSaveReqVO.getDsId());
        if (!isDsStatusSuccess(dsStatusRespDTO1)) {
            throw new ServiceException("上线调度器，失败！");
        }

        // 更新调度器
        iCollectorEtlSchedulerService.updateCollectorEtlScheduler(CollectorEtlSchedulerSaveReqVO);

        // 更新任务状态
        if (!StringUtils.equals("-2", CollectorEtlTaskDO.getStatus()) && !StringUtils.equals("-3", CollectorEtlTaskDO.getStatus())) {
            updateTaskStatus(CollectorEtlTaskDO.getId(), CollectorEtlNewNodeSaveReqVO.getReleaseState());
        } else {
            updateTaskStatus(CollectorEtlTaskDO.getId(), "-3");
        }
        collectorLineageSyncService.lifecycle(CollectorEtlTaskDO, "ACTIVE");
    }

    /**
     * @param releaseState //上下限  0:未上线，1:已上线
     */
    private void wrapCustomNodeStatus(Long id, String releaseState) {
        CollectorEtlTaskRespVO CollectorEtlTaskById = this.getCollectorEtlTaskById(id);
        List<CollectorEtlNodeRespVO> taskDefinitionList = CollectorEtlTaskById.getTaskDefinitionList();

        //循环获取自定义任务
        for (CollectorEtlNodeRespVO CollectorEtlNodeRespVO : taskDefinitionList) {
            buildSubCustomTaskIdList(CollectorEtlNodeRespVO, releaseState);
        }
    }

    private CollectorEtlNewNodeSaveReqVO buildSubCustomTaskIdList(CollectorEtlNodeRespVO CollectorEtlNodeRespVO, String releaseState) {
        String parameters = CollectorEtlNodeRespVO.getParameters();
        Map<String, Object> stringObjectMap = JSONUtils.convertTaskDefinitionJsonMap(parameters);
        long subTaskId = MapUtils.getLongValue(stringObjectMap, "subTaskId");
        CollectorEtlTaskDO CollectorEtlTaskDO = CollectorEtlTaskMapper.selectById(subTaskId);
        if ((StringUtils.equals("0", CollectorEtlTaskDO.getStatus()) || StringUtils.equals("-2", CollectorEtlTaskDO.getStatus()))
                && StringUtils.equals("1", releaseState)) {
            throw new RuntimeException("存在未上线的子工作流，请先将所有子工作流上线");
        }
//
//
//        if(StringUtils.equals("-2",CollectorEtlTaskDO.getStatus()) || StringUtils.equals("-3",CollectorEtlTaskDO.getStatus())){
//            CollectorEtlNewNodeSaveReqVO CollectorEtlNewNodeSaveReqVO = new CollectorEtlNewNodeSaveReqVO();
//            CollectorEtlNewNodeSaveReqVO.setIdStr(String.valueOf(CollectorEtlTaskDO.getId()));
//            return CollectorEtlNewNodeSaveReqVO;
//        }
        return null;
    }

    // 更新任务状态
    private void updateTaskStatus(Long taskId, String releaseState) {
        CollectorEtlTaskSaveReqVO updateReqVO = new CollectorEtlTaskSaveReqVO();
        updateReqVO.setId(taskId);
        updateReqVO.setStatus(releaseState);
        this.updateCollectorEtlTask(updateReqVO);
    }

    private String resolvePublishedDsTaskCode(CollectorEtlTaskDO taskDO) {
        String taskCode = taskDO.getCode();
        CollectorEtlTaskExtDO taskExt = CollectorEtlTaskExtService.getByTaskId(taskDO.getId());
        if (taskExt != null && StringUtils.isNotEmpty(taskExt.getEtlTaskCode())) {
            taskCode = taskExt.getEtlTaskCode();
        }
        if (StringUtils.isEmpty(taskCode)) {
            throw new ServiceException("任务编码为空，请重新发布任务！");
        }
        return taskCode;
    }

    // 创建或更新调度器
    private DsSchedulerRespDTO createOrUpdateScheduler(CollectorEtlSchedulerDO CollectorEtlSchedulerById, CollectorEtlTaskDO CollectorEtlTaskDO) {
        DsSchedulerSaveReqDTO dsSchedulerSaveReqDTO = TaskConverter.createSchedulerRequest(CollectorEtlSchedulerById.getCronExpression(), CollectorEtlTaskDO.getCode(), getProjectWorkerGroup(CollectorEtlTaskDO.getSpaceCode()));
        DsSchedulerRespDTO dsSchedulerRespDTO = iDsEtlSchedulerService.saveScheduler(dsSchedulerSaveReqDTO, String.valueOf(CollectorEtlTaskDO.getSpaceCode()));
        if (dsSchedulerRespDTO == null || !dsSchedulerRespDTO.getSuccess()) {
            DsSchedulerRespDTO byTaskCode = iDsEtlSchedulerService.getByTaskCode(String.valueOf(CollectorEtlTaskDO.getSpaceCode()), CollectorEtlTaskDO.getCode());
            if (byTaskCode == null || !byTaskCode.getSuccess()) {
                throw new ServiceException("创建调度器，失败！");
            }
            Schedule data = byTaskCode.getData();
            DsSchedulerUpdateReqDTO schedulerUpdateRequest = TaskConverter.createSchedulerUpdateRequest(data.getId(), CollectorEtlSchedulerById.getCronExpression(), CollectorEtlTaskDO.getCode(), getProjectWorkerGroup(CollectorEtlTaskDO.getSpaceCode()));
            dsSchedulerRespDTO = iDsEtlSchedulerService.updateScheduler(schedulerUpdateRequest, String.valueOf(CollectorEtlTaskDO.getSpaceCode()));
            if (dsSchedulerRespDTO == null || !dsSchedulerRespDTO.getSuccess()) {
                throw new ServiceException("更新调度器，失败！");
            }
        }
        return dsSchedulerRespDTO;
    }

    // 更新现有调度器
    private DsSchedulerRespDTO updateExistingScheduler(CollectorEtlSchedulerDO CollectorEtlSchedulerById, CollectorEtlTaskDO CollectorEtlTaskDO) {
        DsSchedulerUpdateReqDTO schedulerUpdateRequest = TaskConverter.createSchedulerUpdateRequest(CollectorEtlSchedulerById.getDsId(), CollectorEtlSchedulerById.getCronExpression(), CollectorEtlTaskDO.getCode(), getProjectWorkerGroup(CollectorEtlTaskDO.getSpaceCode()));
        DsSchedulerRespDTO dsSchedulerRespDTO = iDsEtlSchedulerService.updateScheduler(schedulerUpdateRequest, String.valueOf(CollectorEtlTaskDO.getSpaceCode()));
        if (dsSchedulerRespDTO == null || !dsSchedulerRespDTO.getSuccess()) {
            DsSchedulerRespDTO byTaskCode = iDsEtlSchedulerService.getByTaskCode(String.valueOf(CollectorEtlTaskDO.getSpaceCode()), CollectorEtlTaskDO.getCode());
            if (byTaskCode == null || !byTaskCode.getSuccess()) {
                DsSchedulerSaveReqDTO dsSchedulerSaveReqDTO = TaskConverter.createSchedulerRequest(CollectorEtlSchedulerById.getCronExpression(), CollectorEtlTaskDO.getCode(), getProjectWorkerGroup(CollectorEtlTaskDO.getSpaceCode()));
                DsSchedulerRespDTO saveScheduler = iDsEtlSchedulerService.saveScheduler(dsSchedulerSaveReqDTO, String.valueOf(CollectorEtlTaskDO.getSpaceCode()));
                if (saveScheduler == null || !saveScheduler.getSuccess()) {
                    throw new ServiceException("创建调度器，失败！");
                }
                return saveScheduler;
            }
            Schedule data = byTaskCode.getData();
            DsSchedulerUpdateReqDTO updateRequest = TaskConverter.createSchedulerUpdateRequest(data.getId(), CollectorEtlSchedulerById.getCronExpression(), CollectorEtlTaskDO.getCode(), getProjectWorkerGroup(CollectorEtlTaskDO.getSpaceCode()));
            dsSchedulerRespDTO = iDsEtlSchedulerService.updateScheduler(updateRequest, String.valueOf(CollectorEtlTaskDO.getSpaceCode()));
            if (dsSchedulerRespDTO == null || !dsSchedulerRespDTO.getSuccess()) {
                throw new ServiceException("修改调度器，失败！");
            }
        }
        return dsSchedulerRespDTO;
    }


    @Override
    public CollectorEtlTaskRespVO getCollectorEtlTaskById(Long id) {
        CollectorEtlTaskDO CollectorEtlTaskDO = CollectorEtlTaskMapper.selectById(id);
        if (CollectorEtlTaskDO == null) {
            throw new ServiceException("数据集成任务不存在，ID: " + id);
        }
        checkTaskSpaceAccess(CollectorEtlTaskDO);
        fillLastExecuteInfo(CollectorEtlTaskDO);
        CollectorEtlTaskRespVO bean = BeanUtils.toBean(CollectorEtlTaskDO, CollectorEtlTaskRespVO.class);

        List<CollectorEtlTaskNodeRelRespVO> CollectorEtlTaskNodeRelRespVOList = this.getTaskNodeRelList(bean);
        bean.setTaskRelationJson(CollectorEtlTaskNodeRelRespVOList);
        String type = bean.getType();

        List<CollectorEtlNodeRespVO> etlNodeLogRespVOList = this.getNodeRespListByTaskNodeRelList(CollectorEtlTaskNodeRelRespVOList);

        bean.setTaskDefinitionList(removeDuplicateById(etlNodeLogRespVOList, type));
        return bean;
    }


    @Override
    public Map<String, Object> releaseTaskCrontab(CollectorEtlNewNodeSaveReqVO CollectorEtlNewNodeSaveReqVO) {
        CollectorEtlTaskDO CollectorEtlTaskDO = CollectorEtlTaskMapper.selectById(CollectorEtlNewNodeSaveReqVO.getIdStr());
        checkTaskSpaceAccess(CollectorEtlTaskDO);
        String dsTaskCode = CollectorEtlTaskDO.getCode();
        CollectorEtlTaskExtDO taskExt = CollectorEtlTaskExtService.getByTaskId(CollectorEtlTaskDO.getId());
        if (taskExt != null && StringUtils.isNotEmpty(taskExt.getEtlTaskCode())) {
            dsTaskCode = taskExt.getEtlTaskCode();
        }
        if (isStreamingFlinkxTask(taskExt)) {
            disableSchedulerForStreamingTask(CollectorEtlTaskDO, dsTaskCode);
            throw new ServiceException("CDC/流式任务不支持定时调度，请直接发布任务运行");
        }
        CollectorEtlSchedulerDO CollectorEtlSchedulerById = getCollectorEtlScheduler(dsTaskCode, CollectorEtlTaskDO.getId());
        //补偿
        if (CollectorEtlSchedulerById == null || CollectorEtlSchedulerById.getId() == null) {
            CollectorEtlSchedulerSaveReqVO CollectorEtlSchedulerSaveReqVO = TaskConverter.convertToCollectorEtlSchedulerSaveReqVO(CollectorEtlTaskDO.getId(), dsTaskCode, CollectorEtlNewNodeSaveReqVO);
            CollectorEtlSchedulerById = iCollectorEtlSchedulerService.createCollectorEtlSchedulerNew(CollectorEtlSchedulerSaveReqVO);
        }

        if (StringUtils.equals("1", CollectorEtlSchedulerById.getStatus())) {
            throw new ServiceException("调度上线中，不允许改，请先下线！");
        }

        if (StringUtils.isEmpty(CollectorEtlNewNodeSaveReqVO.getCrontab()) ||
                StringUtils.equals(CollectorEtlSchedulerById.getCronExpression(), CollectorEtlNewNodeSaveReqVO.getCrontab())) {
            return new HashMap<>();
        }
        DsSchedulerRespDTO dsSchedulerRespDTO = null;
        CollectorEtlSchedulerSaveReqVO CollectorEtlSchedulerSaveReqVO = new CollectorEtlSchedulerSaveReqVO();
        if (CollectorEtlSchedulerById.getDsId() != null && CollectorEtlSchedulerById.getDsId() > 0) {
            //     * 修改调度器 (只有任务发布了才能调用该接口)
            DsSchedulerUpdateReqDTO schedulerUpdateRequest = TaskConverter.createSchedulerUpdateRequest(CollectorEtlSchedulerById.getDsId(), CollectorEtlNewNodeSaveReqVO.getCrontab(), dsTaskCode, getProjectWorkerGroup(CollectorEtlTaskDO.getSpaceCode()));
            dsSchedulerRespDTO = iDsEtlSchedulerService.updateScheduler(schedulerUpdateRequest, String.valueOf(CollectorEtlTaskDO.getSpaceCode()));
            if (dsSchedulerRespDTO == null || !dsSchedulerRespDTO.getSuccess()) {
                DsSchedulerRespDTO byTaskCode = iDsEtlSchedulerService.getByTaskCode(String.valueOf(CollectorEtlTaskDO.getSpaceCode()), dsTaskCode);
                if (byTaskCode != null && byTaskCode.getSuccess()) {
                    Schedule data = byTaskCode.getData();
                    DsSchedulerUpdateReqDTO updateRequest = TaskConverter.createSchedulerUpdateRequest(data.getId(), CollectorEtlNewNodeSaveReqVO.getCrontab(), dsTaskCode, getProjectWorkerGroup(CollectorEtlTaskDO.getSpaceCode()));
                    dsSchedulerRespDTO = iDsEtlSchedulerService.updateScheduler(updateRequest, String.valueOf(CollectorEtlTaskDO.getSpaceCode()));
                    if (dsSchedulerRespDTO == null || !dsSchedulerRespDTO.getSuccess()) {
                        throw new ServiceException("修改调度器，失败！");
                    }
                }
            }
            if (dsSchedulerRespDTO == null || !dsSchedulerRespDTO.getSuccess()) {
                throw new ServiceException("修改调度器，失败！");
            }
            CollectorEtlTaskDO.setCode(dsTaskCode);
            CollectorEtlSchedulerSaveReqVO = TaskConverter.convertToCollectorEtlSchedulerSaveReqVO(dsSchedulerRespDTO, CollectorEtlTaskDO);
        } else {
            CollectorEtlSchedulerSaveReqVO = new CollectorEtlSchedulerSaveReqVO();
            CollectorEtlSchedulerSaveReqVO.setCronExpression(CollectorEtlNewNodeSaveReqVO.getCrontab());
            CollectorEtlSchedulerSaveReqVO.setTaskCode(dsTaskCode);
        }
        CollectorEtlSchedulerSaveReqVO.setId(CollectorEtlSchedulerById.getId());
        iCollectorEtlSchedulerService.updateCollectorEtlScheduler(CollectorEtlSchedulerSaveReqVO);


        return new HashMap<>();
    }

    @Override
    public CollectorEtlTaskUpdateQueryRespVO getuUpdateQueryInfo(Long id) {
        CollectorEtlTaskDO collectorEtlTaskDO = CollectorEtlTaskMapper.selectById(id);
        if (collectorEtlTaskDO == null) {
            throw new ServiceException("数据集成任务不存在，ID: " + id);
        }
        checkTaskSpaceAccess(collectorEtlTaskDO);
        // 单独查询创建人名称
        MPJLambdaWrapper<CollectorEtlTaskDO> nameWrapper = new MPJLambdaWrapper<>();
        nameWrapper.selectAll(CollectorEtlTaskDO.class)
                .select("(SELECT NICK_NAME FROM SYSTEM_USER WHERE DEL_FLAG = '0' AND CAST(USER_ID AS "
                        + ("mysql".equals(MasterDataSourceConfig.getDatabaseType()) ? "CHAR" : "VARCHAR")
                        + ") = t.PERSON_CHARGE) AS personChargeName")
                .eq(CollectorEtlTaskDO::getId, id);
        CollectorEtlTaskDO nameHolder = CollectorEtlTaskMapper.selectJoinOne(CollectorEtlTaskDO.class, nameWrapper);
        if (nameHolder != null) {
            collectorEtlTaskDO.setPersonChargeName(nameHolder.getPersonChargeName());
        }
        fillLastExecuteInfo(collectorEtlTaskDO);
        List<CollectorEtlTaskNodeRelRespVO> CollectorEtlTaskNodeRelRespVOList = this.getTaskNodeRelList(BeanUtils.toBean(collectorEtlTaskDO, CollectorEtlTaskRespVO.class));

        CollectorEtlTaskUpdateQueryRespVO bean = new CollectorEtlTaskUpdateQueryRespVO(collectorEtlTaskDO);
        bean.setTaskRelationJsonFromNodeRelList(CollectorEtlTaskNodeRelRespVOList);
        String type = bean.getType();

        //获取调度信息
        CollectorEtlSchedulerPageReqVO CollectorEtlSchedulerPageReqVO = new CollectorEtlSchedulerPageReqVO();
        CollectorEtlSchedulerPageReqVO.setTaskCode(bean.getCode());
        CollectorEtlSchedulerPageReqVO.setTaskId(bean.getId());
        CollectorEtlSchedulerDO CollectorEtlSchedulerById = iCollectorEtlSchedulerService.getCollectorEtlSchedulerById(CollectorEtlSchedulerPageReqVO);
        CollectorEtlSchedulerById = CollectorEtlSchedulerById == null ? new CollectorEtlSchedulerDO() : CollectorEtlSchedulerById;
        bean.setCrontab(CollectorEtlSchedulerById.getCronExpression());
        bean.setSchedulerState(CollectorEtlSchedulerById.getStatus());

        List<CollectorEtlNodeRespVO> etlNodeLogRespVOList = this.getNodeRespListByTaskNodeRelList(CollectorEtlTaskNodeRelRespVOList);
        if (etlNodeLogRespVOList.size() > 0) {
            for (CollectorEtlNodeRespVO CollectorEtlNodeRespVO : etlNodeLogRespVOList) {
                if (StringUtils.equals(TaskComponentTypeEnum.DB_READER.getCode(), CollectorEtlNodeRespVO.getComponentType())) {
                    String nodeCode = CollectorEtlNodeRespVO.getCode();
                    com.alibaba.fastjson2.JSONObject taskParams = com.alibaba.fastjson2.JSONObject.parse(CollectorEtlNodeRespVO.getParameters());
                    //读取方式 1:全量 2:id增量 3:时间范围增量 默认全量
                    String readModeType = taskParams.getString("readModeType");
                    if (StringUtils.equals("2", readModeType)) {
                        com.alibaba.fastjson2.JSONObject idIncrementConfig = taskParams.getJSONObject("idIncrementConfig");
                        String incrementColumn = idIncrementConfig.getString("incrementColumn");
                        String cacheKey = TaskConverter.ETL_READER_ID_KEY + nodeCode + ":" + incrementColumn;
                        if (redisService.hasKey(cacheKey)) {
                            idIncrementConfig.put("incrementStart", redisService.get(cacheKey));
                        }
                    } else if (StringUtils.equals("3", readModeType)) {
                        com.alibaba.fastjson2.JSONObject dateIncrementConfig = taskParams.getJSONObject("dateIncrementConfig");
                        List<com.alibaba.fastjson2.JSONObject> columnList = dateIncrementConfig.getJSONArray("column").stream().map(e -> {
                            return (com.alibaba.fastjson2.JSONObject) e;
                        }).collect(Collectors.toList());
                        for (int i = 0; i < columnList.size(); i++) {
                            JSONObject jsonObject = columnList.get(i);
                            //类型  1:固定值 2:时间范围 3:SQL表达式
                            if (!StringUtils.equals("2", jsonObject.getString("type"))) {
                                continue;
                            }
                            String incrementColumn = jsonObject.getString("incrementColumn");
                            String cacheKey = TaskConverter.ETL_READER_DATE_KEY + nodeCode + ":" + incrementColumn;
                            if (redisService.hasKey(cacheKey)) {
                                jsonObject.put("cursorTime", redisService.get(cacheKey));
                            }
                        }
                    }
                    CollectorEtlNodeRespVO.setParameters(taskParams.toJSONString());
                }
            }
        }
        bean.setTaskDefinitionList(removeDuplicateById(etlNodeLogRespVOList, type));
        bean.createTaskConfig();
        return bean;
    }

    private void fillLastExecuteInfo(CollectorEtlTaskDO taskDO) {
        if (taskDO == null || taskDO.getId() == null) {
            return;
        }
        List<CollectorEtlTaskInstanceDO> instances = CollectorEtlTaskInstanceService.list(
                Wrappers.lambdaQuery(CollectorEtlTaskInstanceDO.class)
                        .eq(CollectorEtlTaskInstanceDO::getTaskId, taskDO.getId())
                        .orderByDesc(CollectorEtlTaskInstanceDO::getCreateTime)
                        .last("LIMIT 1"));
        if (CollectionUtils.isEmpty(instances)) {
            return;
        }
        CollectorEtlTaskInstanceDO instance = instances.get(0);
        Date lastExecuteTime = instance.getEndTime() != null
                ? instance.getEndTime()
                : (instance.getStartTime() != null ? instance.getStartTime() : instance.getCreateTime());
        taskDO.setLastExecuteTime(lastExecuteTime);
        taskDO.setLastExecuteStatus(instance.getStatus());
    }


    public List<CollectorEtlNodeRespVO> getNodeRespListByTaskNodeRelList(List<CollectorEtlTaskNodeRelRespVO> CollectorEtlTaskNodeRelRespVOList) {

        if (CollectionUtils.isEmpty(CollectorEtlTaskNodeRelRespVOList)) {
            return new ArrayList<>();
        }

        // 收集所有 preNodeCode 和 postNodeCode
        Set<String> nodeCodeSet = new HashSet<>();
        for (CollectorEtlTaskNodeRelRespVO relVO : CollectorEtlTaskNodeRelRespVOList) {
            if (relVO.getPreNodeCode() != null) {
                nodeCodeSet.add(relVO.getPreNodeCode());
            }
            if (relVO.getPostNodeCode() != null) {
                nodeCodeSet.add(relVO.getPostNodeCode());
            }
        }

        if (CollectionUtils.isEmpty(nodeCodeSet)) {
            return new ArrayList<>();
        }

        // 查询节点信息
        CollectorEtlNodePageReqVO pageReqVO = new CollectorEtlNodePageReqVO();
        pageReqVO.setCodeList(new ArrayList<>(nodeCodeSet));
        return iCollectorEtlNodeService.getCollectorEtlNodeRespList(pageReqVO);
    }

    @Override
    public Long getTaskIdByTaskCode(String taskCode) {
        CollectorEtlTaskDO collectorEtlTaskDO = baseMapper.selectOne(Wrappers.lambdaQuery(CollectorEtlTaskDO.class)
                .eq(CollectorEtlTaskDO::getCode, taskCode)
                .select(CollectorEtlTaskDO::getId));
        if (collectorEtlTaskDO != null) {
            return collectorEtlTaskDO.getId();
        }
        CollectorEtlTaskExtDO taskExt = CollectorEtlTaskExtService.getOne(Wrappers.lambdaQuery(CollectorEtlTaskExtDO.class)
                .eq(CollectorEtlTaskExtDO::getEtlTaskCode, taskCode)
                .select(CollectorEtlTaskExtDO::getTaskId), false);
        if (taskExt != null) {
            return taskExt.getTaskId();
        }
        return null;
    }

    @Override
    public CollectorEtlTaskRespDTO getTaskByTaskCode(String taskCode) {
        CollectorEtlTaskDO collectorEtlTaskDO = baseMapper.selectOne(Wrappers.lambdaQuery(CollectorEtlTaskDO.class)
                .eq(CollectorEtlTaskDO::getCode, taskCode));
        if (collectorEtlTaskDO != null) {
            return BeanUtils.toBean(collectorEtlTaskDO, CollectorEtlTaskRespDTO.class);
        }
        CollectorEtlTaskExtDO taskExt = CollectorEtlTaskExtService.getOne(Wrappers.lambdaQuery(CollectorEtlTaskExtDO.class)
                .eq(CollectorEtlTaskExtDO::getEtlTaskCode, taskCode)
                .select(CollectorEtlTaskExtDO::getTaskId), false);
        if (taskExt == null || taskExt.getTaskId() == null) {
            return null;
        }
        CollectorEtlTaskDO taskByExtCode = baseMapper.selectById(taskExt.getTaskId());
        return taskByExtCode == null ? null : BeanUtils.toBean(taskByExtCode, CollectorEtlTaskRespDTO.class);
    }

    /**
     * 创建请求对象并根据 dsId 获取节点日志
     *
     * @param CollectorEtlNodeLogSaveReqVO 节点日志保存请求对象
     * @return CollectorEtlNodeLogDO 返回节点日志信息
     */
    public CollectorEtlNodeLogDO getCollectorEtlNodeLogByDsId(CollectorEtlNodeLogSaveReqVO CollectorEtlNodeLogSaveReqVO) {
        // 创建请求对象
        CollectorEtlNodeLogPageReqVO reqVO = new CollectorEtlNodeLogPageReqVO();
        reqVO.setDsId(CollectorEtlNodeLogSaveReqVO.getDsId());

        // 调用服务方法获取节点日志信息
        return iCollectorEtlNodeLogService.getCollectorEtlNodeLogRespVOByReqVO(reqVO);
    }

    public CollectorEtlNodeLogDO getCollectorEtlNodeLogByCodeAndVersion(CollectorEtlNodeLogSaveReqVO CollectorEtlNodeLogSaveReqVO) {
        // 创建请求对象
        CollectorEtlNodeLogPageReqVO reqVO = new CollectorEtlNodeLogPageReqVO();
        reqVO.setCode(CollectorEtlNodeLogSaveReqVO.getCode());
        reqVO.setVersion(CollectorEtlNodeLogSaveReqVO.getVersion());

        // 调用服务方法获取节点日志信息
        return iCollectorEtlNodeLogService.getCollectorEtlNodeLogRespVOByReqVO(reqVO);
    }

    /**
     * 创建请求对象并根据任务节点日志信息获取日志
     *
     * @param CollectorEtlTaskNodeRelLogSaveReqVO 任务节点日志保存请求对象
     * @return CollectorEtlTaskNodeRelLogRespVO 返回任务节点日志响应对象
     */
    public CollectorEtlTaskNodeRelLogRespVO getCollectorEtlTaskNodeRelLogByRequest(CollectorEtlTaskNodeRelLogSaveReqVO CollectorEtlTaskNodeRelLogSaveReqVO) {
        // 创建请求对象
        CollectorEtlTaskNodeRelLogPageReqVO reqVO = new CollectorEtlTaskNodeRelLogPageReqVO();
        reqVO.setTaskCode(CollectorEtlTaskNodeRelLogSaveReqVO.getTaskCode());
        reqVO.setTaskVersion(CollectorEtlTaskNodeRelLogSaveReqVO.getTaskVersion());

        List<CollectorEtlTaskNodeRelLogRespVO> CollectorEtlTaskNodeRelLogRespVOList = iCollectorEtlTaskNodeRelLogService.getCollectorEtlTaskNodeRelLogRespVOList(reqVO);
        if (CollectionUtils.isNotEmpty(CollectorEtlTaskNodeRelLogRespVOList)) {
            for (CollectorEtlTaskNodeRelLogRespVO CollectorEtlTaskNodeRelLogRespVO : CollectorEtlTaskNodeRelLogRespVOList) {
                if (CollectorEtlTaskNodeRelLogRespVO != null) {
                    return CollectorEtlTaskNodeRelLogRespVO;
                }
            }
        }

        // 调用服务方法获取任务节点日志信息
        return null;
    }

    /**
     * 创建请求对象并根据任务日志信息获取任务
     *
     * @param CollectorEtlTaskLogSaveReqVO 任务日志保存请求对象
     * @return CollectorEtlTaskLogRespVO 返回任务日志响应对象
     */
    public CollectorEtlTaskLogRespVO getCollectorEtlTaskLogByRequest(CollectorEtlTaskLogSaveReqVO CollectorEtlTaskLogSaveReqVO) {
        // 创建请求对象
        CollectorEtlTaskLogPageReqVO reqVO = new CollectorEtlTaskLogPageReqVO();
        reqVO.setCode(CollectorEtlTaskLogSaveReqVO.getCode());
        reqVO.setVersion(CollectorEtlTaskLogSaveReqVO.getVersion());

        // 调用服务方法获取任务日志信息
        return iCollectorEtlTaskLogService.getCollectorEtlTaskLogById(reqVO);
    }

    @Override
    public AjaxResult startCollectorEtlTask(Long id) {
        CollectorEtlTaskDO CollectorEtlTaskDO = CollectorEtlTaskMapper.selectById(id);
        if (CollectorEtlTaskDO == null) {
            return error("任务不存在，请刷新后重试！");
        }
        checkTaskSpaceAccess(CollectorEtlTaskDO);
        CollectorEtlTaskExtDO runtime = CollectorEtlTaskExtService.getByTaskId(id);
        if (isComponentSnapshot(runtime)) {
            runGuardMapper.lockTask(id);
            CollectorEtlTaskOpsEventDO pendingRecovery = recoveryEvents.latestComponent(id);
            if (pendingRecovery != null && "STARTING".equals(pendingRecovery.getActionStatus())) return error("自动恢复启动请求尚在确认，请勿重复启动");
            if (!StringUtils.equals("1", CollectorEtlTaskDO.getStatus())) return error("组件任务未发布，请先发布后再运行");
            if (runGuardEnabled && !runGuardMapper.findActive(id).isEmpty()) return error("组件任务已有活动或待核对实例，请确认结束后再启动");
        }
        if (!StringUtils.equals("1", CollectorEtlTaskDO.getStatus())) {
            CollectorEtlNewNodeSaveReqVO nodeSaveReqVO = new CollectorEtlNewNodeSaveReqVO();
            nodeSaveReqVO.setIdStr(String.valueOf(id));
            nodeSaveReqVO.setReleaseState("1");
            this.updateReleaseJobTask(nodeSaveReqVO);
//            return error("任务状态错误，请刷新后重试！");
        }
        CollectorEtlTaskDO.setCode(resolvePublishedDsTaskCode(CollectorEtlTaskDO));


        DsStatusRespDTO dsStatusRespDTO = startPublishedWorkflow(CollectorEtlTaskDO, CollectorEtlTaskDO.getCode());

        return Boolean.TRUE.equals(dsStatusRespDTO == null ? null : dsStatusRespDTO.getSuccess())
                ? success() : error(dsStatusRespDTO == null ? "DolphinScheduler无响应" : dsStatusRespDTO.getMsg());
    }


    @Resource private com.datamaster.module.collector.dal.mapper.etl.CollectorEtlTaskOpsEventMapper recoveryEvents;
    @Resource private ICollectorEtlTaskOpsPolicyService recoveryPolicies;
    @Resource @org.springframework.context.annotation.Lazy private CollectorComponentRecoveryService componentRecovery;

    @Override
    public DsStatusRespDTO startComponentRecovery(Long eventId) {
        CollectorEtlTaskOpsEventDO event = recoveryEvents.selectById(eventId);
        if (event == null || !"RESTART_COMPONENT_TASK".equals(event.getAction()) || !"STARTING".equals(event.getActionStatus())) {
            throw new ServiceException("自动恢复事件未取得启动资格");
        }
        runGuardMapper.lockTask(event.getTaskId());
        event = recoveryEvents.lockEvent(eventId);
        if (event == null || !"STARTING".equals(event.getActionStatus())) throw new ServiceException("恢复事件已取消");
        CollectorEtlTaskDO task = CollectorEtlTaskMapper.selectById(event.getTaskId());
        CollectorEtlTaskExtDO ext = CollectorEtlTaskExtService.getByTaskId(event.getTaskId());
        CollectorEtlTaskOpsPolicyDO policy = recoveryPolicies.getByTaskId(event.getTaskId());
        JSONObject recovery = JSON.parseObject(event.getAiRawResult()).getJSONObject("recovery");
        if (recovery == null) throw new ServiceException("恢复元数据无效");
        if (task == null || !"1".equals(task.getStatus()) || !isComponentSnapshot(ext) || policy == null
                || Boolean.TRUE.equals(policy.getFailStopEnabled()) || !Boolean.TRUE.equals(policy.getAiManaged())
                || !Boolean.TRUE.equals(policy.getAutoRecoverEnabled()) || !"SAFE_AUTO".equals(policy.getRecoverStrategy())
                || !Objects.equals(ext.getEtlTaskCode(), recovery.getString("workflowCode"))
                || !Objects.equals(String.valueOf(ext.getEtlTaskVersion()), recovery.getString("workflowVersion"))
                || !runGuardMapper.findActive(task.getId()).isEmpty()) throw new ServiceException("恢复条件已变化，禁止启动");
        CollectorEtlTaskInstanceDO latestRun = runtimeInstanceMapper.latestMainInstance(event.getTaskId());
        if (latestRun != null && !Objects.equals(latestRun.getId(), recovery.getLong("beforeExecutionId"))) {
            throw new ServiceException("已有更新执行，取消自动重复提交");
        }
        return startPublishedWorkflow(task, ext.getEtlTaskCode());
    }

    private DsStatusRespDTO startPublishedWorkflow(CollectorEtlTaskDO task, String workflowCode) {
        DsStartTaskReqDTO request = TaskConverter.createDsStartTaskReqDTO(workflowCode, getProjectWorkerGroup(task.getSpaceCode()));
        return dsEtlTaskService.startTask(request, task.getSpaceCode());
    }

    @Override
    public List<CollectorEtlTaskTreeRespVO> getCollectorEtlTaskListTree(CollectorEtlTaskPageReqVO reqVO) {
        MPJLambdaWrapper<CollectorEtlTaskDO> wrapper = new MPJLambdaWrapper<>();
        wrapper.selectAll(CollectorEtlTaskDO.class)
                .ne(CollectorEtlTaskDO::getStatus, "-2")
                .ne(CollectorEtlTaskDO::getStatus, "-3")
                .eq(reqVO.getSpaceId() != null, CollectorEtlTaskDO::getSpaceId, reqVO.getSpaceId())
                .eq(StringUtils.isNotBlank(reqVO.getSpaceCode()), CollectorEtlTaskDO::getSpaceCode, reqVO.getSpaceCode())
                .ne(CollectorEtlTaskDO::getType, "4");
        List<CollectorEtlTaskDO> CollectorEtlTaskDOS = CollectorEtlTaskMapper.selectList(wrapper);
        List<CollectorEtlTaskRespVO> CollectorEtlTaskRespVOList = BeanUtils.toBean(CollectorEtlTaskDOS, CollectorEtlTaskRespVO.class);

        List<TaxonomyCategoryDO> attTaskCatApiList = taxonomyCategoryService.getCategoryList(CatType.TASK.getValue());
        List<TaxonomyCategoryDO> attDataDevCatApiList = taxonomyCategoryService.getCategoryList(CatType.DATA_DEV.getValue());

        List<CollectorEtlTaskTreeRespVO> result = new ArrayList<>();
        CollectorEtlTaskTreeRespVO builtTaskCatTree = buildTaskCatTree(CollectorEtlTaskRespVOList, attTaskCatApiList);
        CollectorEtlTaskTreeRespVO builtTaskDevCaTree = buildTaskDevCaTree(CollectorEtlTaskRespVOList, attDataDevCatApiList);


        result.add(builtTaskCatTree);
        result.add(builtTaskDevCaTree);
        return result;
    }

    private CollectorEtlTaskTreeRespVO buildTaskDevCaTree(List<CollectorEtlTaskRespVO> CollectorEtlTaskRespVOList, List<TaxonomyCategoryDO> attDataDevCatApiList) {

        // 1. 构造顶级目录节点
        CollectorEtlTaskTreeRespVO root = new CollectorEtlTaskTreeRespVO();
        root.setId(IdUtils.generateArtificialId());
        root.setTreeId(IdUtils.generateArtificialId());
        root.setLabel("数据开发");
        root.setChildren(new ArrayList<>());

        // 2. 整理类目列表：先将每个 AttTaskCatRespDTO 转换为 CollectorEtlTaskTreeRespVO 节点，并放入 map（key 为类目 id）
        Map<Long, CollectorEtlTaskTreeRespVO> catNodeMap = new HashMap<>();
        for (TaxonomyCategoryDO cat : attDataDevCatApiList) {
            CollectorEtlTaskTreeRespVO node = new CollectorEtlTaskTreeRespVO();
            // 重新生成一个 id
            node.setTreeId(IdUtils.generateArtificialId());
            node.setId(cat.getId());
            node.setLabel(cat.getName());
            node.setCode(cat.getCode());
            node.setChildren(new ArrayList<>());
            // 类目节点的 CollectorEtlTaskCount 后续会赋值
            catNodeMap.put(cat.getId(), node);
        }

        // 3. 构建类目的层级关系，根据 parentId 建立树形结构
        List<CollectorEtlTaskTreeRespVO> catRoots = new ArrayList<>();
        for (TaxonomyCategoryDO cat : attDataDevCatApiList) {
            CollectorEtlTaskTreeRespVO node = catNodeMap.get(cat.getId());
            if (cat.getParentId() != null && catNodeMap.containsKey(cat.getParentId())) {
                // 如果存在父类，则加入父类的 children
                CollectorEtlTaskTreeRespVO parentNode = catNodeMap.get(cat.getParentId());
                parentNode.getChildren().add(node);
            } else {
                // 无父类，则为根级类目
                catRoots.add(node);
            }
        }

        // 4. 从任务列表中过滤出 type 为 "3" 的任务
        List<CollectorEtlTaskRespVO> filteredTasks = CollectorEtlTaskRespVOList.stream()
                .filter(task -> "3".equals(task.getType()))
                .collect(Collectors.toList());

        root.setCollectorEtlTaskCount(filteredTasks.size());

        // 为便于根据类目 code 查找类目节点，构建一个 code 到节点的映射
        Map<String, CollectorEtlTaskTreeRespVO> catCodeMap = new HashMap<>();
        for (CollectorEtlTaskTreeRespVO catNode : catNodeMap.values()) {
            catCodeMap.put(catNode.getCode(), catNode);
        }

        // 5. 遍历任务，将每个任务挂载到对应的类目节点下（匹配条件：任务的 catCode 与类目节点的 code 相等）
        for (CollectorEtlTaskRespVO task : filteredTasks) {
            String taskCatCode = task.getCatCode();
            if (taskCatCode == null) {
                continue;
            }
            CollectorEtlTaskTreeRespVO categoryNode = catCodeMap.get(taskCatCode);
            if (categoryNode != null) {
                // 将任务转换为树节点
                CollectorEtlTaskTreeRespVO taskNode = new CollectorEtlTaskTreeRespVO();
                taskNode.setTreeId(IdUtils.generateArtificialId());
                taskNode.setId(task.getId());
                taskNode.setLabel(task.getName());
                taskNode.setType(task.getType());
                taskNode.setName(task.getName());
                taskNode.setCode(task.getCode());
                taskNode.setVersion(task.getVersion());
                taskNode.setSpaceId(task.getSpaceId());
                taskNode.setSpaceCode(task.getSpaceCode());
                taskNode.setPersonCharge(task.getPersonCharge());
                taskNode.setContactNumber(task.getContactNumber());
                taskNode.setLocations(task.getLocations());
                taskNode.setDescription(task.getDescription());
                taskNode.setExecutionType(task.getExecutionType());
                taskNode.setStatus(task.getStatus());
                taskNode.setDsId(task.getDsId());
                taskNode.setChildren(new ArrayList<>());

                // 将任务节点加入对应类目节点的 children
                categoryNode.getChildren().add(taskNode);
            }
        }
//
//        // 6. 为每个类目节点赋值任务数量（CollectorEtlTaskCount 仅统计直接挂载的任务数量）
//        for (CollectorEtlTaskTreeRespVO catNode : catNodeMap.values()) {
//            int taskCount = 0;
//            // 这里判断子节点中，type 非空的节点视为任务节点
//            for (CollectorEtlTaskTreeRespVO child : catNode.getChildren()) {
//                if (child.getType() != null) {
//                    taskCount++;
//                }
//            }
//            catNode.setCollectorEtlTaskCount(taskCount);
//        }

        // 6. 将整理好的类目树挂载到顶级目录下
        root.getChildren().addAll(catRoots);

        // 7. 递归计算每个节点的任务数量（包括子节点所有任务）
        computeTaskCount(root);
        // 返回顶级目录节点列表（只有一个根节点）
        return root;
    }

    /**
     * 构建数据集成任务树
     *
     * @param CollectorEtlTaskRespVOList 任务列表（其中 catCode 存储的是 AttTaskCatRespDTO 的 code）
     * @param attTaskCatApiList    类目列表，AttTaskCatRespDTO 中存在上下级关系，示例：父级 code "A01"，下级第一个 code "A01A01"
     * @return List<CollectorEtlTaskTreeRespVO> 构造后的任务树，顶级目录为“数据集成”
     */
    public CollectorEtlTaskTreeRespVO buildTaskCatTree(List<CollectorEtlTaskRespVO> CollectorEtlTaskRespVOList,
                                                 List<TaxonomyCategoryDO> attTaskCatApiList) {
        // 1. 构造顶级目录节点
        CollectorEtlTaskTreeRespVO root = new CollectorEtlTaskTreeRespVO();
        // 重新生成一个 id
        root.setTreeId(IdUtils.generateArtificialId());
        root.setId(IdUtils.generateArtificialId());
        root.setLabel("数据集成");
        root.setChildren(new ArrayList<>());

        // 2. 整理类目列表：先将每个 AttTaskCatRespDTO 转换为 CollectorEtlTaskTreeRespVO 节点，并放入 map（key 为类目 id）
        Map<Long, CollectorEtlTaskTreeRespVO> catNodeMap = new HashMap<>();
        for (TaxonomyCategoryDO cat : attTaskCatApiList) {
            CollectorEtlTaskTreeRespVO node = new CollectorEtlTaskTreeRespVO();
            // 重新生成一个 id
            node.setTreeId(IdUtils.generateArtificialId());
            node.setId(cat.getId());
            node.setLabel(cat.getName());
            node.setCode(cat.getCode());
            node.setChildren(new ArrayList<>());
            // 类目节点的 CollectorEtlTaskCount 后续会赋值
            catNodeMap.put(cat.getId(), node);
        }

        // 3. 构建类目的层级关系，根据 parentId 建立树形结构
        List<CollectorEtlTaskTreeRespVO> catRoots = new ArrayList<>();
        for (TaxonomyCategoryDO cat : attTaskCatApiList) {
            CollectorEtlTaskTreeRespVO node = catNodeMap.get(cat.getId());
            if (cat.getParentId() != null && catNodeMap.containsKey(cat.getParentId())) {
                // 如果存在父类，则加入父类的 children
                CollectorEtlTaskTreeRespVO parentNode = catNodeMap.get(cat.getParentId());
                parentNode.getChildren().add(node);
            } else {
                // 无父类，则为根级类目
                catRoots.add(node);
            }
        }

        // 4. 从任务列表中过滤出 type 为 "1" 或 "2" 的任务
        List<CollectorEtlTaskRespVO> filteredTasks = CollectorEtlTaskRespVOList.stream()
                .filter(task -> "1".equals(task.getType()) || "2".equals(task.getType()))
                .collect(Collectors.toList());

        root.setCollectorEtlTaskCount(filteredTasks.size());

        // 为便于根据类目 code 查找类目节点，构建一个 code 到节点的映射
        Map<String, CollectorEtlTaskTreeRespVO> catCodeMap = new HashMap<>();
        for (CollectorEtlTaskTreeRespVO catNode : catNodeMap.values()) {
            catCodeMap.put(catNode.getCode(), catNode);
        }

        // 5. 遍历任务，将每个任务挂载到对应的类目节点下（匹配条件：任务的 catCode 与类目节点的 code 相等）
        for (CollectorEtlTaskRespVO task : filteredTasks) {
            String taskCatCode = task.getCatCode();
            if (taskCatCode == null) {
                continue;
            }
            CollectorEtlTaskTreeRespVO categoryNode = catCodeMap.get(taskCatCode);
            if (categoryNode != null) {
                CollectorEtlTaskExtDO etlTaskExtDO = CollectorEtlTaskExtService.getByTaskId(task.getId());
                // 将任务转换为树节点
                CollectorEtlTaskTreeRespVO taskNode = new CollectorEtlTaskTreeRespVO();
                // 重新生成一个 id
                taskNode.setTreeId(IdUtils.generateArtificialId());
                taskNode.setId(task.getId());
                taskNode.setLabel(task.getName());
                taskNode.setType(task.getType());
                taskNode.setName(task.getName());
                taskNode.setCode(task.getCode());
                if (etlTaskExtDO != null) {
                    taskNode.setExtCode(etlTaskExtDO.getEtlTaskCode());
                }
                taskNode.setVersion(task.getVersion());
                taskNode.setSpaceId(task.getSpaceId());
                taskNode.setSpaceCode(task.getSpaceCode());
                taskNode.setPersonCharge(task.getPersonCharge());
                taskNode.setContactNumber(task.getContactNumber());
                taskNode.setLocations(task.getLocations());
                taskNode.setDescription(task.getDescription());
                taskNode.setExecutionType(task.getExecutionType());
                taskNode.setStatus(task.getStatus());
                taskNode.setDsId(task.getDsId());
                taskNode.setChildren(new ArrayList<>());

                // 将任务节点加入对应类目节点的 children
                categoryNode.getChildren().add(taskNode);
            }
        }
//
//        // 6. 为每个类目节点赋值任务数量（CollectorEtlTaskCount 仅统计直接挂载的任务数量）
//        for (CollectorEtlTaskTreeRespVO catNode : catNodeMap.values()) {
//            int taskCount = 0;
//            // 这里判断子节点中，type 非空的节点视为任务节点
//            for (CollectorEtlTaskTreeRespVO child : catNode.getChildren()) {
//                if (child.getType() != null) {
//                    taskCount++;
//                }
//            }
//            catNode.setCollectorEtlTaskCount(taskCount);
//        }

        // 6. 将整理好的类目树挂载到顶级目录下
        root.getChildren().addAll(catRoots);

        // 7. 递归计算每个节点的任务数量（包括子节点所有任务）
        computeTaskCount(root);

        // 返回顶级目录节点列表（只有一个根节点）
        return root;
    }


    /**
     * 递归计算节点的任务数量，并赋值给 CollectorEtlTaskCount
     * 如果节点为任务节点（type != null），计数为 1；
     * 如果节点为类目节点（type == null），计数为其所有子节点任务数量之和
     *
     * @param node 当前节点
     * @return 当前节点及其子节点的任务总数
     */
    private static int computeTaskCount(CollectorEtlTaskTreeRespVO node) {
        int count = 0;
        // 如果是任务节点，计数为 1
        if (node.getType() != null) {
            count = 1;
        }
        // 如果存在子节点，则递归累加
        if (node.getChildren() != null && !node.getChildren().isEmpty()) {
            for (CollectorEtlTaskTreeRespVO child : node.getChildren()) {
                count += computeTaskCount(child);
            }
        }
        node.setCollectorEtlTaskCount(count);
        return count;
    }

    @Override
    public int checkTaskIdInDatasource(List<Long> datasourceIdList, List<Long> spaceIdList) {
        return CollectorEtlTaskMapper.checkTaskIdInDatasource(datasourceIdList, spaceIdList);
    }

    @Override
    public int checkTaskIdInAsset(List<Long> assetIdList) {
        return CollectorEtlTaskMapper.checkTaskIdInAsset(assetIdList);
    }

    @Override
    public List<CollectorEtlTaskRespDTO> listVisibleTasks(Long spaceId, String spaceCode,
                                                           String name, String status, int limit) {
        CollectorEtlTaskPageReqVO request = new CollectorEtlTaskPageReqVO();
        request.setPageNum(1);
        request.setPageSize(Math.max(1, Math.min(limit <= 0 ? 20 : limit, 100)));
        request.setType("3");
        request.setSpaceId(spaceId);
        request.setSpaceCode(spaceCode);
        request.setName(StringUtils.isBlank(name) ? null : name.trim());
        request.setStatus(StringUtils.isBlank(status) ? null : status.trim());
        PageResult<CollectorEtlTaskRespVO> page = getCollectorEtlTaskPageList(request);
        if (page == null || page.getRows() == null) return new ArrayList<>();
        return BeanUtils.toBean(page.getRows(), CollectorEtlTaskRespDTO.class);
    }

    @Override
    public CollectorEtlTaskRespDTO getVisibleTask(Long id, Long spaceId, String spaceCode) {
        if (id == null) return null;
        CollectorEtlTaskDO task = CollectorEtlTaskMapper.selectById(id);
        if (!visibleTask(task, spaceId, spaceCode)) return null;
        return BeanUtils.toBean(task, CollectorEtlTaskRespDTO.class);
    }

    @Override
    public List<CollectorEtlNodeRespDTO> listVisibleTaskNodes(Long taskId, Long spaceId, String spaceCode) {
        CollectorEtlTaskDO task = taskId == null ? null : CollectorEtlTaskMapper.selectById(taskId);
        if (!visibleTask(task, spaceId, spaceCode)) return new ArrayList<>();
        CollectorEtlTaskNodeRelPageReqVO relationRequest = new CollectorEtlTaskNodeRelPageReqVO();
        relationRequest.setTaskId(taskId);
        relationRequest.setTaskCode(task.getCode());
        relationRequest.setTaskVersion(task.getVersion());
        List<CollectorEtlTaskNodeRelRespVO> relations = iCollectorEtlTaskNodeRelService
                .getCollectorEtlTaskNodeRelRespVOList(relationRequest);
        Set<String> nodeCodes = new LinkedHashSet<>();
        if (relations != null) for (CollectorEtlTaskNodeRelRespVO relation : relations) {
            if (relation == null) continue;
            if (StringUtils.isNotBlank(relation.getPreNodeCode())) nodeCodes.add(relation.getPreNodeCode());
            if (StringUtils.isNotBlank(relation.getPostNodeCode())) nodeCodes.add(relation.getPostNodeCode());
        }
        CollectorEtlNodePageReqVO nodeRequest = new CollectorEtlNodePageReqVO();
        nodeRequest.setCodeList(new ArrayList<>(nodeCodes));
        List<CollectorEtlNodeRespVO> nodes = nodeCodes.isEmpty()
                ? new ArrayList<>() : iCollectorEtlNodeService.getCollectorEtlNodeRespList(nodeRequest);
        if (nodes == null) return new ArrayList<>();
        return BeanUtils.toBean(nodes.stream()
                .filter(node -> node != null
                        && Objects.equals(spaceId, node.getSpaceId())
                        && StringUtils.equals(spaceCode, node.getSpaceCode())
                        && StringUtils.equals("3", node.getTaskType()))
                .collect(Collectors.toList()), CollectorEtlNodeRespDTO.class);
    }

    @Override
    public List<CollectorEtlTaskNodeRelRespDTO> listVisibleTaskRelations(Long taskId, Long spaceId, String spaceCode) {
        CollectorEtlTaskDO task = taskId == null ? null : CollectorEtlTaskMapper.selectById(taskId);
        if (!visibleTask(task, spaceId, spaceCode)) return new ArrayList<>();
        CollectorEtlTaskNodeRelPageReqVO request = new CollectorEtlTaskNodeRelPageReqVO();
        request.setTaskId(taskId);
        request.setTaskCode(task.getCode());
        request.setTaskVersion(task.getVersion());
        List<CollectorEtlTaskNodeRelRespVO> relations = iCollectorEtlTaskNodeRelService
                .getCollectorEtlTaskNodeRelRespVOList(request);
        if (relations == null) return new ArrayList<>();
        return BeanUtils.toBean(relations.stream()
                .filter(rel -> rel != null
                        && Objects.equals(spaceId, rel.getSpaceId())
                        && StringUtils.equals(spaceCode, rel.getSpaceCode())
                        && Objects.equals(taskId, rel.getTaskId()))
                .collect(Collectors.toList()), CollectorEtlTaskNodeRelRespDTO.class);
    }

    @Override
    public Map<String, Object> applyVisibleTaskPlan(Long taskId, Long spaceId, String spaceCode, String planJson) {
        CollectorEtlTaskDO task = taskId == null ? null : CollectorEtlTaskMapper.selectById(taskId);
        if (!visibleTask(task, spaceId, spaceCode)) {
            throw new ServiceException("数据开发任务不存在或不属于当前空间");
        }
        if (StringUtils.equals("1", task.getStatus()) || StringUtils.equals("-3", task.getStatus())) {
            throw new ServiceException("上线任务不允许修改，请先下线");
        }
        if (StringUtils.isBlank(planJson)) throw new ServiceException("计划内容不能为空");

        JSONObject plan;
        try {
            plan = JSON.parseObject(planJson);
        } catch (Exception ex) {
            throw new ServiceException("计划 JSON 格式错误");
        }
        if (plan == null) throw new ServiceException("计划 JSON 必须是对象");
        JSONArray nodes = plan.getJSONArray("nodes");
        if (nodes == null) nodes = plan.getJSONArray("taskDefinitionList");
        if (nodes == null || nodes.isEmpty()) throw new ServiceException("计划至少需要一个节点");

        List<Map<String, Object>> nodePayload = new ArrayList<>();
        Set<String> nodeCodes = new LinkedHashSet<>();
        for (Object raw : nodes) {
            if (!(raw instanceof JSONObject)) throw new ServiceException("节点定义必须是对象");
            JSONObject source = (JSONObject) raw;
            String code = firstText(source, "code", "nodeCode", "id");
            String name = firstText(source, "name", "label");
            String componentType = firstText(source, "taskType", "componentType", "type");
            if (StringUtils.isBlank(code) || StringUtils.isBlank(name) || StringUtils.isBlank(componentType)) {
                throw new ServiceException("节点必须包含 code、name、type");
            }
            if (!nodeCodes.add(code)) throw new ServiceException("节点编码重复: " + code);
            Map<String, Object> target = new LinkedHashMap<>();
            target.put("id", longOrNull(source.get("id")));
            target.put("code", code);
            target.put("name", name);
            target.put("taskType", componentType);
            target.put("type", componentType);
            target.put("componentType", firstText(source, "componentType", "type", "taskType"));
            target.put("version", numberOrDefault(source.get("version"), 1));
            target.put("description", firstText(source, "description", "desc"));
            target.put("priority", firstText(source, "priority"));
            target.put("parameters", JSON.toJSONString(parameterObject(source)));
            target.put("taskParams", parameterObject(source));
            nodePayload.add(target);
        }

        JSONArray relations = plan.getJSONArray("relations");
        if (relations == null) relations = plan.getJSONArray("taskRelationJson");
        List<Map<String, Object>> relationPayload = new ArrayList<>();
        if (relations != null) for (Object raw : relations) {
            if (!(raw instanceof JSONObject)) throw new ServiceException("依赖关系必须是对象");
            JSONObject source = (JSONObject) raw;
            String pre = firstText(source, "preTaskCode", "preNodeCode", "from", "pre", "source");
            String post = firstText(source, "postTaskCode", "postNodeCode", "to", "post", "target");
            if (StringUtils.isBlank(pre) || StringUtils.isBlank(post)
                    || !nodeCodes.contains(pre) || !nodeCodes.contains(post)) {
                throw new ServiceException("依赖关系引用了不存在的节点");
            }
            Map<String, Object> target = new LinkedHashMap<>();
            target.put("preTaskCode", pre);
            target.put("postTaskCode", post);
            target.put("preTaskVersion", numberOrDefault(source.get("preTaskVersion"), 1));
            target.put("postTaskVersion", numberOrDefault(source.get("postTaskVersion"), 1));
            target.put("conditionType", source.get("conditionType"));
            relationPayload.add(target);
        }

        Long spaceCodeValue;
        try {
            spaceCodeValue = Long.valueOf(String.valueOf(task.getSpaceCode()));
        } catch (Exception ex) {
            throw new ServiceException("当前空间编码不是数字，无法复用数据开发保存接口");
        }
        CollectorEtlNewNodeSaveReqVO request = new CollectorEtlNewNodeSaveReqVO();
        request.setIdStr(String.valueOf(taskId));
        request.setSpaceId(spaceId);
        request.setSpaceCode(spaceCodeValue);
        request.setType("3");
        // 保留任务原有的负责人、联系方式和执行配置，确保保存任务日志时满足
        // COL_ETL_TASK_LOG 的非空约束，并避免应用计划覆盖任务元数据。
        request.setCatId(task.getCatId());
        request.setCatCode(task.getCatCode());
        String personCharge = task.getPersonCharge();
        if (StringUtils.isBlank(personCharge)) {
            personCharge = task.getCreatorId() == null
                    ? String.valueOf(SecurityUtils.getUserId())
                    : String.valueOf(task.getCreatorId());
        }
        request.setPersonCharge(personCharge);
        request.setContactNumber(task.getContactNumber());
        request.setExecutionType(task.getExecutionType());
        request.setTimeout(task.getTimeout());
        request.setName(firstText(plan, "name", "taskName") == null ? task.getName() : firstText(plan, "name", "taskName"));
        request.setDescription(plan.getString("description") == null ? task.getDescription() : plan.getString("description"));
        request.setTaskDefinitionList(JSON.toJSONString(nodePayload));
        request.setTaskRelationJson(JSON.toJSONString(relationPayload));
        request.setGlobalParams(plan.getString("globalParams") == null ? "[]" : plan.getString("globalParams"));
        request.setDraftJson(plan.getString("draftJson") == null ? planJson : plan.getString("draftJson"));
        request.setCrontab(plan.getString("crontab"));
        request.setReleaseState("0");
        request.setSchedulerState("0");
        request.setStatus("0");
        CollectorEtlTaskSaveReqVO saved = updateEtlTask(request);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("taskId", String.valueOf(taskId));
        response.put("success", true);
        response.put("published", false);
        response.put("status", saved == null ? "0" : saved.getStatus());
        response.put("message", "数据开发计划已保存为草稿，尚未发布或启动");
        return response;
    }

    private String firstText(JSONObject source, String... keys) {
        for (String key : keys) {
            Object value = source.get(key);
            if (value != null && StringUtils.isNotBlank(String.valueOf(value))) return String.valueOf(value).trim();
        }
        return null;
    }

    private Object parameterObject(JSONObject source) {
        Object value = source.get("parameters");
        if (value == null) value = source.get("params");
        if (value == null) value = source.get("taskParams");
        if (value instanceof String) {
            try {
                Object parsed = JSON.parse((String) value);
                return parsed == null ? new LinkedHashMap<>() : parsed;
            } catch (Exception ignored) {
                return new LinkedHashMap<>();
            }
        }
        return value == null ? new LinkedHashMap<>() : value;
    }

    private Long longOrNull(Object value) {
        if (value == null || StringUtils.isBlank(String.valueOf(value))) return null;
        try { return Long.valueOf(String.valueOf(value)); } catch (Exception ignored) { return null; }
    }

    private Integer numberOrDefault(Object value, int fallback) {
        if (value == null) return fallback;
        try { return Integer.valueOf(String.valueOf(value)); } catch (Exception ignored) { return fallback; }
    }

    @Override
    public Map<String, Object> startVisibleTask(Long id, Long spaceId, String spaceCode) {
        CollectorEtlTaskDO task = id == null ? null : CollectorEtlTaskMapper.selectById(id);
        if (!visibleTask(task, spaceId, spaceCode)) {
            throw new ServiceException("数据开发任务不存在或不属于当前空间");
        }
        if (!StringUtils.equals("3", task.getType())) {
            throw new ServiceException("当前任务不是数据开发任务");
        }
        AjaxResult result = startCollectorEtlTask(id);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("taskId", String.valueOf(id));
        response.put("success", result != null && result.isSuccess());
        response.put("message", result == null ? null : result.get("msg"));
        return response;
    }

    private boolean visibleTask(CollectorEtlTaskDO task, Long spaceId, String spaceCode) {
        if (task == null || Boolean.TRUE.equals(task.getDelFlag())) return false;
        if (!StringUtils.equals("3", task.getType())) return false;
        if (spaceId != null && !Objects.equals(spaceId, task.getSpaceId())) return false;
        return StringUtils.isBlank(spaceCode) || StringUtils.equals(spaceCode, task.getSpaceCode());
    }

    @Override
    public CollectorEtlNewNodeSaveReqVO createEtlTaskFront(CollectorEtlNewNodeSaveReqVO CollectorEtlNewNodeSaveReqVO) {
        if (StringUtils.equals("1", CollectorEtlNewNodeSaveReqVO.getType())) {
            normalizeProjectCode(CollectorEtlNewNodeSaveReqVO);
            CollectorEtlTaskDO scope = new CollectorEtlTaskDO();
            scope.setSpaceId(CollectorEtlNewNodeSaveReqVO.getSpaceId());
            scope.setSpaceCode(String.valueOf(CollectorEtlNewNodeSaveReqVO.getSpaceCode()));
            if (scope.getSpaceId() == null || CollectorEtlNewNodeSaveReqVO.getSpaceCode() == null) {
                throw new ServiceException("新建组件任务必须指定有效空间");
            }
            checkTaskSpaceAccess(scope);
        }
        //生成任务编码(本地唯一ID，发布时替换为DS编码)
        String taskCode = String.valueOf(IdUtils.generateArtificialId());

        CollectorEtlTaskSaveReqVO createReqVO = new CollectorEtlTaskSaveReqVO();
        createReqVO.setName(CollectorEtlNewNodeSaveReqVO.getName());
        createReqVO.setType(CollectorEtlNewNodeSaveReqVO.getType());
        createReqVO.setCatCode(CollectorEtlNewNodeSaveReqVO.getCatCode());
        createReqVO.setSpaceId(CollectorEtlNewNodeSaveReqVO.getSpaceId());
        createReqVO.setSpaceCode(String.valueOf(CollectorEtlNewNodeSaveReqVO.getSpaceCode()));
        // 创建人(负责人)未传时默认为当前登录用户，避免 col_etl_task.person_charge 非空约束导致新建任务失败
        if (StringUtils.isBlank(CollectorEtlNewNodeSaveReqVO.getPersonCharge())) {
            CollectorEtlNewNodeSaveReqVO.setPersonCharge(String.valueOf(SecurityUtils.getUserId()));
        }
        createReqVO.setPersonCharge(CollectorEtlNewNodeSaveReqVO.getPersonCharge());
        createReqVO.setContactNumber(CollectorEtlNewNodeSaveReqVO.getContactNumber());
        createReqVO.setDescription(CollectorEtlNewNodeSaveReqVO.getDescription());
        createReqVO.setExecutionType(CollectorEtlNewNodeSaveReqVO.getExecutionType());
        createReqVO.setDraftJson(CollectorEtlNewNodeSaveReqVO.getDraftJson());

        //默认
        createReqVO.setCode(taskCode);
        createReqVO.setStatus("-1");//草稿
        createReqVO.setLocations("");
        createReqVO.setTimeout(0L);
        createReqVO.setDsId(0L);

        Long CollectorEtlTask = this.createCollectorEtlTask(createReqVO);

        CollectorEtlSchedulerSaveReqVO CollectorEtlSchedulerSaveReqVO = new CollectorEtlSchedulerSaveReqVO();
        CollectorEtlSchedulerSaveReqVO.setId(IdUtils.generateArtificialId());
        CollectorEtlSchedulerSaveReqVO.setTaskId(CollectorEtlTask);
        CollectorEtlSchedulerSaveReqVO.setTaskCode(taskCode);
        // 获取100年后的时间
        long currentTime = System.currentTimeMillis();
        Date date = new Date(currentTime + 100L * 365 * 24 * 60 * 60 * 1000);
        CollectorEtlSchedulerSaveReqVO.setStartTime(new Date());
        CollectorEtlSchedulerSaveReqVO.setEndTime(date);
        CollectorEtlSchedulerSaveReqVO.setTimezoneId("Asia/Shanghai"); // 默认时区
        CollectorEtlSchedulerSaveReqVO.setCronExpression(CollectorEtlNewNodeSaveReqVO.getCrontab());
        CollectorEtlSchedulerSaveReqVO.setFailureStrategy("1");
        CollectorEtlSchedulerSaveReqVO.setStatus("0");
        CollectorEtlSchedulerSaveReqVO.setDsId((long) -1);
        iCollectorEtlSchedulerService.createCollectorEtlScheduler(CollectorEtlSchedulerSaveReqVO);

        CollectorEtlNewNodeSaveReqVO.setIdStr(String.valueOf(CollectorEtlTask));
        CollectorEtlNewNodeSaveReqVO.setStatus("-1");
        CollectorEtlNewNodeSaveReqVO.setCode(taskCode);
        return CollectorEtlNewNodeSaveReqVO;
    }

    /**
     * @param CollectorEtlNewNodeSaveReqVO
     * @return
     */
    @Override
    public CollectorEtlTaskSaveReqVO createEtlTaskFrontPostposition(CollectorEtlNewNodeSaveReqVO CollectorEtlNewNodeSaveReqVO) {
        //任务类型;1：离线任务 2：实时任务 3：数据开发任务 4：	作业任务
        String type = CollectorEtlNewNodeSaveReqVO.getType();
        if (StringUtils.equals("1", type)) {
            return saveEtlTaskLocal(CollectorEtlNewNodeSaveReqVO);
        } else if (StringUtils.equals("2", type)) {
            return createEtlTaskFrontPostpositionRealTime(CollectorEtlNewNodeSaveReqVO);
        } else if (StringUtils.equals("3", type)) {
            return saveEtlTaskLocal(CollectorEtlNewNodeSaveReqVO);
        } else if (StringUtils.equals("4", type)) {
            return saveEtlTaskLocal(CollectorEtlNewNodeSaveReqVO);
        }
        return null;
    }

    private CollectorEtlTaskSaveReqVO createEtlTaskFrontPostpositionRealTime(CollectorEtlNewNodeSaveReqVO CollectorEtlNewNodeSaveReqVO) {
        return null;
    }

    // ========== 本地保存（不调DS） ==========

    /**
     * 保存ETL任务配置到本地数据库，不调用DS接口
     */
    private CollectorEtlTaskSaveReqVO saveEtlTaskLocal(CollectorEtlNewNodeSaveReqVO reqVO) {
        Long taskId = resolveTaskId(reqVO);
        CollectorEtlTaskDO taskDO = CollectorEtlTaskMapper.selectById(taskId);
        if (taskDO == null) {
            throw new ServiceException("任务不存在");
        }
        String taskCode = taskDO.getCode();
        boolean componentSave = ComponentGraphCompiler.supports(reqVO) || (StringUtils.isNotBlank(taskDO.getDraftJson())
                && JSON.parseObject(taskDO.getDraftJson()).get("componentGraph") instanceof Map);
        if (componentSave) {
            if (runGuardEnabled && isComponentSnapshot(CollectorEtlTaskExtService.getByTaskId(taskId))) {
                runGuardMapper.lockTask(taskId);
                taskDO = CollectorEtlTaskMapper.selectById(taskId);
                if (taskDO == null) throw new ServiceException("任务不存在");
            }
            checkTaskSpaceAccess(taskDO);
            bindComponentTaskOwner(reqVO, taskDO);
            if (StringUtils.equals("1", taskDO.getStatus()) || StringUtils.equals("-3", taskDO.getStatus())) {
                throw new ServiceException("组件任务已发布，请下线后再修改");
            }
        }

        CollectorEtlTaskSaveReqVO taskSaveReqVO = BeanUtils.toBean(taskDO, CollectorEtlTaskSaveReqVO.class);
        taskSaveReqVO.setName(reqVO.getName());
        taskSaveReqVO.setLocations(JSON.toJSONString(reqVO.getLocations()));
        taskSaveReqVO.setDescription(reqVO.getDescription());
        taskSaveReqVO.setStatus("0");
        taskSaveReqVO.setDraftJson(reqVO.getDraftJson());

        //处理节点数据
        List<CollectorEtlNodeSaveReqVO> nodeList = JSON.parseArray(reqVO.getTaskDefinitionList(), CollectorEtlNodeSaveReqVO.class);
        if (componentSave) {
            Set<String> ownedCodes = new HashSet<>();
            for (CollectorEtlTaskNodeRelDO relation : iCollectorEtlTaskNodeRelService.list(
                    Wrappers.lambdaQuery(CollectorEtlTaskNodeRelDO.class).eq(CollectorEtlTaskNodeRelDO::getTaskId, taskId))) {
                ownedCodes.add(relation.getPreNodeCode()); ownedCodes.add(relation.getPostNodeCode());
            }
            if (nodeList == null || nodeList.isEmpty()) throw new ServiceException("组件任务没有可保存节点");
            for (CollectorEtlNodeSaveReqVO node : nodeList) {
                if (node == null || StringUtils.isBlank(node.getCode())) throw new ServiceException("组件缺少稳定标识");
                Long existingId = iCollectorEtlNodeService.getNodeIdByNodeCode(node.getCode());
                if (existingId != null && !ownedCodes.contains(node.getCode())) {
                    throw new ServiceException("组件标识已属于其他任务，请重新添加该组件");
                }
                // 不信任页面提交的本地节点主键，更新身份只从本任务已有节点解析。
                node.setId(existingId);
            }
        }
        List<CollectorEtlNodeSaveReqVO> newTaskDefinitionLogs = new ArrayList<>();
        List<CollectorEtlNodeSaveReqVO> updateTaskDefinitionLogs = new ArrayList<>();
        List<CollectorEtlNodeDO> nodeDOList = new ArrayList<>();

        Map<String, CollectorEtlNodeSaveReqVO> nodeMap = nodeList.stream().collect(Collectors.toMap(CollectorEtlNodeSaveReqVO::getCode, node -> node));

        for (CollectorEtlNodeSaveReqVO createReqVO : nodeList) {
            createReqVO.setType(createReqVO.getTaskType());
            createReqVO.setTaskType(reqVO.getType());
            if (createReqVO.getVersion() == 0) {
                createReqVO.setVersion(1);
            }
            createReqVO.setSpaceId(reqVO.getSpaceId());
            createReqVO.setSpaceCode(String.valueOf(reqVO.getSpaceCode()));
            createReqVO.setParameters(JSON.toJSONString(createReqVO.getTaskParams()));

            CollectorEtlNodeLogDO nodeCodeAndVersion = iCollectorEtlNodeLogService.getByNodeCodeAndVersion(
                    createReqVO.getCode(), createReqVO.getVersion());
            if (nodeCodeAndVersion == null) {
                if (componentSave && createReqVO.getId() != null) {
                    throw new ServiceException("组件版本与已保存节点不一致，请重新打开任务后保存");
                }
                createReqVO.setCreatorId(reqVO.getCreatorId());
                createReqVO.setCreateBy(reqVO.getCreateBy());
                createReqVO.setCreateTime(reqVO.getCreateTime());
                newTaskDefinitionLogs.add(createReqVO);
                continue;
            } else {
                if (!componentSave && StringUtils.equals(TaskComponentTypeEnum.DB_READER.getCode(), String.valueOf(createReqVO.getTaskParams().get("type"))) &&
                        StringUtils.equals("2", String.valueOf(createReqVO.getTaskParams().get("readModeType")))) {
                    JSONObject idIncrementConfig = JSONObject.parseObject(String.valueOf(createReqVO.getTaskParams().get("idIncrementConfig")));
                    String incrementColumn = idIncrementConfig.getString("incrementColumn");
                    Integer incrementStart = idIncrementConfig.getInteger("incrementStart");
                    String cacheKey = TaskConverter.ETL_READER_ID_KEY + createReqVO.getCode() + ":" + incrementColumn;
                    if (redisService.hasKey(cacheKey) && Integer.parseInt(redisService.get(cacheKey)) != incrementStart) {
                        redisService.delete(cacheKey);
                    }
                }
                createReqVO.setUpdatorId(reqVO.getUpdatorId());
                createReqVO.setUpdateBy(reqVO.getUpdateBy());
                createReqVO.setUpdateTime(reqVO.getUpdateTime());
            }

            if (createReqVO.equals(nodeCodeAndVersion)) {
                nodeDOList.add(BeanUtils.toBean(createReqVO, CollectorEtlNodeDO.class));
                continue;
            }

            Integer version = iCollectorEtlNodeLogService.getMaxVersionByNodeCode(createReqVO.getCode());
            createReqVO.setVersion(version + 1);
            updateTaskDefinitionLogs.add(createReqVO);
        }

        //新增节点日志
        List<CollectorEtlNodeSaveReqVO> newInsertTaskDefinitionLogs = newTaskDefinitionLogs.stream()
                .filter(td -> !updateTaskDefinitionLogs.contains(td))
                .collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(newInsertTaskDefinitionLogs)) {
            List<CollectorEtlNodeLogSaveReqVO> saveNodeList = TaskConverter.convertToCollectorEtlNodeLogSaveReqVOList(newInsertTaskDefinitionLogs);
            saveNodeList.forEach(log -> log.setId(null));
            iCollectorEtlNodeLogService.createCollectorEtlNodeLogBatch(TaskConverter.convertToCollectorEtlNodeLogSaveReqVOList(newInsertTaskDefinitionLogs));
        }
        if (CollectionUtils.isNotEmpty(updateTaskDefinitionLogs)) {
            List<CollectorEtlNodeLogSaveReqVO> saveNodeList = TaskConverter.convertToCollectorEtlNodeLogSaveReqVOList(newInsertTaskDefinitionLogs);
            saveNodeList.forEach(log -> log.setId(null));
            iCollectorEtlNodeLogService.createCollectorEtlNodeLogBatch(TaskConverter.convertToCollectorEtlNodeLogSaveReqVOList(updateTaskDefinitionLogs));
        }

        //新增节点数据
        if (CollectionUtils.isNotEmpty(newTaskDefinitionLogs)) {
            nodeDOList.addAll(iCollectorEtlNodeService.createCollectorEtlNodeBatch(newTaskDefinitionLogs));
        }
        if (CollectionUtils.isNotEmpty(updateTaskDefinitionLogs)) {
            for (CollectorEtlNodeSaveReqVO td : updateTaskDefinitionLogs) {
                CollectorEtlNodeDO nodeDO = BeanUtils.toBean(td, CollectorEtlNodeDO.class);
                nodeDOList.add(nodeDO);
                iCollectorEtlNodeService.update(nodeDO, Wrappers.lambdaUpdate(CollectorEtlNodeDO.class)
                        .eq(CollectorEtlNodeDO::getCode, td.getCode()));
            }
        }

        //处理关系数据
        List<CollectorEtlTaskNodeRelSaveReqVO> taskRelationList = TaskConverter.convertToCollectorEtlTaskNodeRelSaveReqVOList(nodeDOList, reqVO, taskSaveReqVO);
        iCollectorEtlTaskNodeRelService.removeOldCollectorEtlTaskNodeRel(taskCode);
        List<CollectorEtlTaskNodeRelSaveReqVO> newRelations = TaskConverter.convertToCollectorEtlTaskNodeRelSaveReqVOList(nodeDOList, reqVO, taskSaveReqVO);
        iCollectorEtlTaskNodeRelService.createCollectorEtlTaskNodeRelBatch(newRelations);
        List<CollectorEtlTaskNodeRelLogSaveReqVO> relLogs = TaskConverter.convertToCollectorEtlTaskNodeRelLogSaveReqVOList(newRelations);
        relLogs.forEach(rl -> rl.setId(null));
        iCollectorEtlTaskNodeRelLogService.createCollectorEtlTaskNodeRelLogBatch(relLogs);

        //更新任务记录
        this.updateCollectorEtlTask(taskSaveReqVO);

        //更新调度器
        CollectorEtlSchedulerDO schedulerDO = getCollectorEtlScheduler(taskCode, taskDO.getId());
        CollectorEtlSchedulerSaveReqVO schedulerSaveReqVO = TaskConverter.convertToCollectorEtlSchedulerSaveReqVO(
                taskDO.getId(), taskCode, reqVO);
        if (schedulerDO != null && schedulerDO.getId() != null) {
            schedulerSaveReqVO.setId(schedulerDO.getId());
            iCollectorEtlSchedulerService.updateCollectorEtlScheduler(schedulerSaveReqVO);
        } else {
            iCollectorEtlSchedulerService.createCollectorEtlScheduler(schedulerSaveReqVO);
        }

        //创建任务日志
        CollectorEtlTaskLogSaveReqVO taskLogSaveReqVO = TaskConverter.fromCollectorEtlTaskLogSaveReqVO(reqVO, taskSaveReqVO);
        taskLogSaveReqVO.setCode(taskCode);
        iCollectorEtlTaskLogService.createCollectorEtlTaskLog(taskLogSaveReqVO);

        //创建扩展数据(占位，发布时填充DS编码)
        CollectorEtlTaskExtDO taskExt = CollectorEtlTaskExtService.getByTaskId(taskDO.getId());
        if (taskExt == null) {
            CollectorEtlTaskExtSaveReqVO extSaveReqVO = CollectorEtlTaskExtSaveReqVO.builder()
                    .taskId(taskDO.getId())
                    .etlNodeName(reqVO.getName())
                    .build();
            CollectorEtlTaskExtService.createCollectorEtlTaskExt(extSaveReqVO);
        }

        return taskSaveReqVO;
    }

    // ========== 发布/卸载 ==========

    @Override
    public CollectorEtlTaskSaveReqVO publishTask(CollectorEtlNewNodeSaveReqVO reqVO) {
        Long taskId = resolveTaskId(reqVO);
        CollectorEtlTaskDO taskDO = CollectorEtlTaskMapper.selectById(taskId);
        if (taskDO == null) {
            throw new ServiceException("任务不存在");
        }
        checkTaskSpaceAccess(taskDO);
        if (StringUtils.equals("1", taskDO.getStatus()) || StringUtils.equals("-3", taskDO.getStatus())) {
            throw new ServiceException("任务已发布，无需重复发布");
        }
        reqVO = fillPublishPayload(reqVO);
        if (ComponentGraphCompiler.supports(reqVO)) bindComponentTaskOwner(reqVO, taskDO);
        if (StringUtils.isBlank(reqVO.getTaskDefinitionList())) {
            throw new ServiceException("任务配置未完成，请先保存任务配置");
        }

        // 只编译发布副本，数据库继续保存用户的六组件画布。
        if (ComponentGraphCompiler.supports(reqVO)) {
            canonicalizeComponentDatasources(reqVO, taskDO);
            CollectorEtlNewNodeSaveReqVO compiled = ComponentGraphCompiler.compile(reqVO);
            // 发布快照保留原始组件拓扑，执行载荷使用独立编译结果。
            return publishPipelineTask(compiled, taskDO);
        }
        // 历史封装式逻辑节点继续使用原发布路径。
        if (TaskConverter.hasPipelineNodes(reqVO.getTaskDefinitionList())) {
            return publishPipelineTask(reqVO, taskDO);
        }

        CollectorEtlTaskExtDO taskExt = CollectorEtlTaskExtService.getByTaskId(taskDO.getId());
        boolean hasPublishedExt = taskExt != null && StringUtils.isNotEmpty(taskExt.getEtlTaskCode());
        ProcessDefinition existingDsDefinition = hasPublishedExt ? null
                : dsEtlTaskService.getTaskByName(String.valueOf(reqVO.getSpaceCode()), reqVO.getName());
        boolean isFirstPublish = !hasPublishedExt;
        boolean createDsDefinition = isFirstPublish
                && (existingDsDefinition == null || StringUtils.isEmpty(existingDsDefinition.getCode()));

        String nodeCode;
        String nodeName;
        if (isFirstPublish) {
            DsNodeGenCodeRespDTO dsNodeGenCodeRespDTO = dsEtlNodeService.genCode(reqVO.getSpaceCode());
            nodeCode = String.valueOf(dsNodeGenCodeRespDTO.getData().get(0));
            nodeName = reqVO.getName() + "-" + DateUtil.today();
        } else {
            nodeCode = taskExt.getEtlNodeCode();
            nodeName = StringUtils.isNotBlank(taskExt.getEtlNodeName()) ? taskExt.getEtlNodeName() : reqVO.getName() + "-" + DateUtil.today();
        }

        DsTaskSaveReqDTO dsTaskSaveReqDTO = new DsTaskSaveReqDTO();
        dsTaskSaveReqDTO.setName(reqVO.getName());
        dsTaskSaveReqDTO.setDescription(reqVO.getDescription());
        dsTaskSaveReqDTO.setExecutionType(StringUtils.isNotBlank(reqVO.getExecutionType()) ? reqVO.getExecutionType() : "SERIAL_WAIT");

        Map<String, Object> mainArgs = null;
        Map<String, Object> taskInfo = new HashMap<>();
        List<DsResource> resourceList = new ArrayList<>();
        String taskDefinition;
        String taskRelation;
        String locations;
        String flinkxJobJson = null;
        boolean streamingFlinkx = false;
        boolean isFlinkx = TaskConverter.isFlinkxEngine(reqVO.getDraftJson());
        FlinkxIncrementalConfig incrementalConfig = null;
        String prepareNodeCode = null;
        String prepareNodeName = reqVO.getName() + "-增量边界准备";
        String completeNodeCode = null;
        String completeNodeName = reqVO.getName() + "-状态回写";
        String taskCode;
        String conditionNodeCode = null;
        String conditionNodeName = reqVO.getName() + "-条件分支";
        String failedCompleteNodeCode = null;
        String failedCompleteNodeName = reqVO.getName() + "-失败状态回写";

        if (isFirstPublish) {
            //首次发布：本地没有已发布扩展；如果DS中已有同名定义，则复用定义编码走更新
            if (existingDsDefinition != null && StringUtils.isNotEmpty(existingDsDefinition.getCode())) {
                taskCode = existingDsDefinition.getCode();
            } else {
                DsNodeGenCodeRespDTO dsTaskGenCodeRespDTO = dsEtlNodeService.genCode(reqVO.getSpaceCode());
                taskCode = String.valueOf(dsTaskGenCodeRespDTO.getData().get(0));
            }

            taskInfo.put("spaceCode", reqVO.getSpaceCode());
            taskInfo.put("taskCode", taskCode);
            taskInfo.put("taskVersion", existingDsDefinition == null ? 1 : existingDsDefinition.getVersion() + 1);
            taskInfo.put("name", reqVO.getName());

            mainArgs = TaskConverter.buildEtlTaskParams(reqVO.getTaskDefinitionList(), new HashMap<>(), taskInfo, resourceList);

            if (TaskConverter.isShellTask(reqVO.getDraftJson())) {
                taskDefinition = TaskConverter.buildShellTaskDefinitionJson(null, nodeName, nodeCode, 0, mainArgs, reqVO.getDraftJson(), getProjectWorkerGroup(reqVO.getSpaceCode()));
            } else if (isFlinkx) {
                flinkxJobJson = FlinkxEtlTaskConverter.convertToFlinkxJobJson(mainArgs);
                streamingFlinkx = TaskConverter.isStreamingFlinkxJob(flinkxJobJson);
                incrementalConfig = TaskConverter.resolveFlinkxIncrementalConfig(mainArgs);
                completeNodeCode = nextDsNodeCode(reqVO.getSpaceCode());
                conditionNodeCode = nextDsNodeCode(reqVO.getSpaceCode());
                failedCompleteNodeCode = nextDsNodeCode(reqVO.getSpaceCode());
                if (incrementalConfig != null) {
                    prepareNodeCode = nextDsNodeCode(reqVO.getSpaceCode());
                    taskDefinition = TaskConverter.buildIncrementalFlinkxTaskDefinitionJson(
                             null, prepareNodeName, prepareNodeCode, 0,
                             null, nodeName, nodeCode, 0,
                             null, conditionNodeName, conditionNodeCode, 0,
                             null, completeNodeName, completeNodeCode, 0,
                             null, failedCompleteNodeName, failedCompleteNodeCode, 0,
                             incrementalCallbackUrl(incrementalPrepareUrl, taskDO.getId()),
                             incrementalCallbackUrl(incrementalCompleteUrl, taskDO.getId(), 1),
                             incrementalCallbackUrl(incrementalCompleteUrl, taskDO.getId(), 0),
                             reqVO.getDraftJson(),
                             getProjectWorkerGroup(reqVO.getSpaceCode()));
                } else {
                    taskDefinition = TaskConverter.buildFlinkxTaskDefinitionJsonWithCompleteCallback(
                             null, nodeName, nodeCode, 0, flinkxJobJson,
                             null, conditionNodeName, conditionNodeCode, 0,
                             null, completeNodeName, completeNodeCode, 0,
                             null, failedCompleteNodeName, failedCompleteNodeCode, 0,
                             incrementalCallbackUrl(incrementalCompleteUrl, taskDO.getId(), 1),
                             incrementalCallbackUrl(incrementalCompleteUrl, taskDO.getId(), 0),
                             reqVO.getDraftJson(), getProjectWorkerGroup(reqVO.getSpaceCode()));
                }
            } else {
                taskDefinition = TaskConverter.buildEtlTaskDefinitionJson(null, nodeName, nodeCode, 0, mainArgs, reqVO.getDraftJson(), getProjectWorkerGroup(reqVO.getSpaceCode()));
            }

            if (incrementalConfig != null) {
                taskRelation = TaskConverter.buildIncrementalFlinkxTaskRelationJson(
                        null, null, null, null, null,
                        prepareNodeCode, 0, nodeCode, 0, conditionNodeCode, 0, completeNodeCode, 0, failedCompleteNodeCode, 0);
                locations = TaskConverter.buildIncrementalFlinkxTaskLocationsJson(
                        reqVO.getLocations(), prepareNodeCode, nodeCode, conditionNodeCode, completeNodeCode, failedCompleteNodeCode);
            } else if (isFlinkx) {
                taskRelation = TaskConverter.buildFlinkxTaskRelationWithCompleteCallback(
                        (Long) null, (Long) null, (Long) null, (Long) null, nodeCode, 0, conditionNodeCode, 0, completeNodeCode, 0, failedCompleteNodeCode, 0);
                locations = TaskConverter.buildFlinkxTaskLocationsWithCompleteCallback(
                        reqVO.getLocations(), nodeCode, conditionNodeCode, completeNodeCode, failedCompleteNodeCode);
            } else {
                taskRelation = TaskConverter.buildEtlTaskRelationJson(null, nodeCode);
                locations = TaskConverter.buildEtlTaskLocationsJson(reqVO.getLocations(), nodeCode);
            }

            if (runGuardEnabled && isFlinkx) {
                List<Map<String, Object>> guardedDefinitions = (List) JSON.parseArray(taskDefinition, Map.class);
                List<Map<String, Object>> guardedRelations = (List) JSON.parseArray(taskRelation, Map.class);
                List<Map<String, Object>> guardedLocations = (List) JSON.parseArray(locations, Map.class);
                addWorkflowRunGuard(reqVO, guardedDefinitions, guardedRelations, guardedLocations);
                taskDefinition = JSON.toJSONString(guardedDefinitions);
                taskRelation = JSON.toJSONString(guardedRelations);
                locations = JSON.toJSONString(guardedLocations);
            }
            dsTaskSaveReqDTO.setTaskDefinitionJson(taskDefinition);
            dsTaskSaveReqDTO.setTaskRelationJson(taskRelation);
            dsTaskSaveReqDTO.setLocations(locations);

            //创建或恢复更新DS任务
            DsTaskSaveRespDTO task = createDsDefinition
                    ? dsEtlTaskService.createTask(dsTaskSaveReqDTO, reqVO.getSpaceCode())
                    : updateDsTaskAllowingOnline(dsTaskSaveReqDTO, String.valueOf(reqVO.getSpaceCode()), taskCode);
            if (!task.getSuccess()) {
                throw new ServiceException("发布任务错误:" + task.getMsg());
            }
            ProcessDefinition data = task.getData();
            TaskDefinition dsFlinkxNode = getDsTaskDefinition(data, nodeCode);
            ProcessTaskRelation dsFlinkxRelation = getDsTaskRelation(data, nodeCode);

            //保存扩展数据
            CollectorEtlTaskExtSaveReqVO extSaveReqVO = CollectorEtlTaskExtSaveReqVO.builder()
                    .taskId(taskDO.getId())
                    .etlTaskCode(data.getCode())
                    .etlTaskVersion(data.getVersion())
                    .etlNodeId(dsFlinkxNode.getId())
                    .etlNodeName(nodeName)
                    .etlNodeCode(nodeCode)
                    .etlNodeVersion(dsFlinkxNode.getVersion())
                    .etlRelationId(dsFlinkxRelation.getId())
                    .build();
            if (isFlinkx) {
                extSaveReqVO.setFlinkxJobJson(flinkxJobJson);
                fillCompleteNodeExt(extSaveReqVO, data, completeNodeCode, completeNodeName);
            }
            fillIncrementalExt(extSaveReqVO, incrementalConfig, flinkxJobJson, data,
                    prepareNodeCode, prepareNodeName, completeNodeCode, completeNodeName);
            if (taskExt != null) {
                copyPublishedExt(extSaveReqVO, taskExt);
                if (incrementalConfig == null) {
                    clearIncrementalExt(taskExt);
                    if (isFlinkx) {
                        fillCompleteNodeExt(taskExt, data, completeNodeCode, completeNodeName);
                    }
                }
                CollectorEtlTaskExtService.updateById(taskExt);
            } else {
                CollectorEtlTaskExtService.createCollectorEtlTaskExt(extSaveReqVO);
            }

            taskCode = data.getCode(); //用于后续scheduler/release
        } else {
            //二次发布：使用已有DS任务编码
            taskCode = taskExt.getEtlTaskCode();
            int nodeVersion = taskExt.getEtlNodeVersion() == null ? 0 : taskExt.getEtlNodeVersion();

            taskInfo.put("spaceCode", reqVO.getSpaceCode());
            taskInfo.put("taskCode", taskCode);
            taskInfo.put("taskVersion", taskExt.getEtlTaskVersion() != null ? taskExt.getEtlTaskVersion() + 1 : 1);
            taskInfo.put("name", reqVO.getName());

            mainArgs = TaskConverter.buildEtlTaskParams(reqVO.getTaskDefinitionList(), new HashMap<>(), taskInfo, resourceList);

            if (TaskConverter.isShellTask(reqVO.getDraftJson())) {
                taskDefinition = TaskConverter.buildShellTaskDefinitionJson(taskExt.getEtlNodeId(), nodeName, nodeCode, nodeVersion, mainArgs, reqVO.getDraftJson(), getProjectWorkerGroup(reqVO.getSpaceCode()));
            } else if (isFlinkx) {
                flinkxJobJson = FlinkxEtlTaskConverter.convertToFlinkxJobJson(mainArgs);
                streamingFlinkx = TaskConverter.isStreamingFlinkxJob(flinkxJobJson);
                incrementalConfig = TaskConverter.resolveFlinkxIncrementalConfig(mainArgs);
                completeNodeCode = StringUtils.isNotBlank(taskExt.getCompleteNodeCode())
                        ? taskExt.getCompleteNodeCode() : nextDsNodeCode(reqVO.getSpaceCode());
                conditionNodeCode = nextDsNodeCode(reqVO.getSpaceCode());
                failedCompleteNodeCode = nextDsNodeCode(reqVO.getSpaceCode());
                if (incrementalConfig != null) {
                    prepareNodeCode = StringUtils.isNotBlank(taskExt.getPrepareNodeCode())
                            ? taskExt.getPrepareNodeCode() : nextDsNodeCode(reqVO.getSpaceCode());
                    taskDefinition = TaskConverter.buildIncrementalFlinkxTaskDefinitionJson(
                             taskExt.getPrepareNodeId(), prepareNodeName, prepareNodeCode,
                             taskExt.getPrepareNodeVersion(), taskExt.getEtlNodeId(), nodeName,
                             nodeCode, taskExt.getEtlNodeVersion(),
                             null, conditionNodeName, conditionNodeCode, 0,
                             taskExt.getCompleteNodeId(), completeNodeName, completeNodeCode,
                             taskExt.getCompleteNodeVersion(),
                             null, failedCompleteNodeName, failedCompleteNodeCode, 0,
                             incrementalCallbackUrl(incrementalPrepareUrl, taskDO.getId()),
                             incrementalCallbackUrl(incrementalCompleteUrl, taskDO.getId(), 1),
                             incrementalCallbackUrl(incrementalCompleteUrl, taskDO.getId(), 0),
                             reqVO.getDraftJson(),
                             getProjectWorkerGroup(reqVO.getSpaceCode()));
                } else {
                    taskDefinition = TaskConverter.buildFlinkxTaskDefinitionJsonWithCompleteCallback(
                             taskExt.getEtlNodeId(), nodeName, nodeCode, nodeVersion, flinkxJobJson,
                             null, conditionNodeName, conditionNodeCode, 0,
                             taskExt.getCompleteNodeId(), completeNodeName, completeNodeCode,
                             taskExt.getCompleteNodeVersion(),
                             null, failedCompleteNodeName, failedCompleteNodeCode, 0,
                             incrementalCallbackUrl(incrementalCompleteUrl, taskDO.getId(), 1),
                             incrementalCallbackUrl(incrementalCompleteUrl, taskDO.getId(), 0),
                             reqVO.getDraftJson(), getProjectWorkerGroup(reqVO.getSpaceCode()));
                }
            } else {
                taskDefinition = TaskConverter.buildEtlTaskDefinitionJson(taskExt.getEtlNodeId(), nodeName, nodeCode, nodeVersion, mainArgs, reqVO.getDraftJson(), getProjectWorkerGroup(reqVO.getSpaceCode()));
            }

            if (incrementalConfig != null) {
                taskRelation = TaskConverter.buildIncrementalFlinkxTaskRelationJson(
                        taskExt.getPrepareRelationId(), taskExt.getEtlRelationId(), null, taskExt.getCompleteRelationId(), null,
                         prepareNodeCode, taskExt.getPrepareNodeVersion(),
                         nodeCode, taskExt.getEtlNodeVersion(),
                         conditionNodeCode, 0,
                         completeNodeCode, taskExt.getCompleteNodeVersion(),
                         failedCompleteNodeCode, 0);
                locations = TaskConverter.buildIncrementalFlinkxTaskLocationsJson(
                        reqVO.getLocations(), prepareNodeCode, nodeCode, conditionNodeCode, completeNodeCode, failedCompleteNodeCode);
            } else if (isFlinkx) {
                taskRelation = TaskConverter.buildFlinkxTaskRelationWithCompleteCallback(
                        taskExt.getEtlRelationId(), (Long) null, taskExt.getCompleteRelationId(), (Long) null,
                        nodeCode, nodeVersion, conditionNodeCode, 0, completeNodeCode, taskExt.getCompleteNodeVersion(),
                        failedCompleteNodeCode, 0);
                locations = TaskConverter.buildFlinkxTaskLocationsWithCompleteCallback(
                        reqVO.getLocations(), nodeCode, conditionNodeCode, completeNodeCode, failedCompleteNodeCode);
            } else {
                taskRelation = TaskConverter.buildEtlTaskRelationJson(taskExt.getEtlRelationId(), nodeCode, nodeVersion);
                locations = TaskConverter.buildEtlTaskLocationsJson(reqVO.getLocations(), nodeCode);
            }

            if (runGuardEnabled && isFlinkx) {
                List<Map<String, Object>> guardedDefinitions = (List) JSON.parseArray(taskDefinition, Map.class);
                List<Map<String, Object>> guardedRelations = (List) JSON.parseArray(taskRelation, Map.class);
                List<Map<String, Object>> guardedLocations = (List) JSON.parseArray(locations, Map.class);
                addWorkflowRunGuard(reqVO, guardedDefinitions, guardedRelations, guardedLocations);
                taskDefinition = JSON.toJSONString(guardedDefinitions);
                taskRelation = JSON.toJSONString(guardedRelations);
                locations = JSON.toJSONString(guardedLocations);
            }
            dsTaskSaveReqDTO.setTaskDefinitionJson(taskDefinition);
            dsTaskSaveReqDTO.setTaskRelationJson(taskRelation);
            dsTaskSaveReqDTO.setLocations(locations);

            //更新DS任务
            DsTaskSaveRespDTO task = updateDsTaskAllowingOnline(dsTaskSaveReqDTO, String.valueOf(reqVO.getSpaceCode()), taskCode);
            if (!task.getSuccess()) {
                throw new ServiceException("发布任务错误:" + task.getMsg());
            }
            ProcessDefinition data = task.getData();
            TaskDefinition dsFlinkxNode = getDsTaskDefinition(data, nodeCode);
            ProcessTaskRelation dsFlinkxRelation = getDsTaskRelation(data, nodeCode);

            //更新扩展数据
            taskExt.setEtlTaskVersion(data.getVersion());
            taskExt.setEtlNodeId(dsFlinkxNode.getId());
            taskExt.setEtlNodeVersion(dsFlinkxNode.getVersion());
            taskExt.setEtlRelationId(dsFlinkxRelation.getId());
            if (isFlinkx) {
                taskExt.setFlinkxJobJson(flinkxJobJson);
                fillCompleteNodeExt(taskExt, data, completeNodeCode, completeNodeName);
            }
            fillIncrementalExt(taskExt, incrementalConfig, flinkxJobJson, data,
                    prepareNodeCode, prepareNodeName, completeNodeCode, completeNodeName);
            CollectorEtlTaskExtService.updateById(taskExt);
            createTaskLogSnapshotIfAbsent(reqVO, data, taskCode, locations, taskDO.getDsId());
        }

        //上线DS任务
        DsStatusRespDTO releaseResp = dsEtlTaskService.releaseTask("ONLINE", String.valueOf(reqVO.getSpaceCode()), taskCode);
        String responseMsg = releaseResp == null ? null : releaseResp.getMsg();
        if (responseMsg != null && responseMsg.contains("SubWorkflowDefinition") && responseMsg.contains("is not online")) {
            throw new RuntimeException("存在未上线的子工作流，请先将所有子工作流上线");
        }
        if (!isDsStatusSuccess(releaseResp)) {
            throw new ServiceException("上线任务失败！");
        }

        //更新任务状态为已上线
        updateTaskStatus(taskDO.getId(), "1");
        taskDO.setStatus("1");
        taskDO.setCode(taskCode); //确保使用DS编码

        // Capture the exact generated executable job, not editable draft transitions.
        mainArgs.put("publishedJobJson", flinkxJobJson);
        writeTaskLineageSilently(taskDO, mainArgs);

        if (streamingFlinkx) {
            disableSchedulerForStreamingTask(taskDO, taskCode);
            return BeanUtils.toBean(taskDO, CollectorEtlTaskSaveReqVO.class);
        }

        //调度器处理
        CollectorEtlSchedulerDO schedulerDO = getCollectorEtlScheduler(taskCode, taskDO.getId());
        if (schedulerDO == null || schedulerDO.getId() == null) {
            schedulerDO = getCollectorEtlScheduler(null, taskDO.getId());
        }
        if ((schedulerDO == null || schedulerDO.getId() == null) && StringUtils.isNotBlank(reqVO.getCrontab())) {
            CollectorEtlSchedulerSaveReqVO schedulerSaveReqVO = TaskConverter.convertToCollectorEtlSchedulerSaveReqVO(
                    taskDO.getId(), taskCode, reqVO
            );
            schedulerDO = iCollectorEtlSchedulerService.createCollectorEtlSchedulerNew(schedulerSaveReqVO);
        }
        if (schedulerDO != null && schedulerDO.getId() != null && StringUtils.isBlank(schedulerDO.getCronExpression())
                && StringUtils.isNotBlank(reqVO.getCrontab())) {
            schedulerDO.setCronExpression(reqVO.getCrontab());
        }
        if (schedulerDO != null && StringUtils.isNotEmpty(schedulerDO.getCronExpression())) {
            try {
                DsSchedulerRespDTO dsSchedulerRespDTO;
                if (schedulerDO.getDsId() == null || schedulerDO.getDsId() < 1) {
                    dsSchedulerRespDTO = createOrUpdateScheduler(schedulerDO, taskDO);
                } else {
                    dsSchedulerRespDTO = updateExistingScheduler(schedulerDO, taskDO);
                }
                if (dsSchedulerRespDTO != null && dsSchedulerRespDTO.getData() != null) {
                    CollectorEtlSchedulerSaveReqVO schedulerSaveReqVO = TaskConverter.convertToCollectorEtlSchedulerSaveReqVO(dsSchedulerRespDTO, taskDO);
                    schedulerSaveReqVO.setId(schedulerDO.getId());
                    schedulerSaveReqVO.setTaskCode(taskCode);
                    DsStatusRespDTO onlineScheduler = iDsEtlSchedulerService.onlineScheduler(taskDO.getSpaceCode(), schedulerSaveReqVO.getDsId());
                    if (!isDsStatusSuccess(onlineScheduler)) {
                        throw new ServiceException("上线调度器失败！");
                    }
                    schedulerSaveReqVO.setStatus("1");
                    iCollectorEtlSchedulerService.updateCollectorEtlScheduler(schedulerSaveReqVO);
                }
            } catch (Exception e) {
                if (schedulerDO.getId() != null) {
                    CollectorEtlSchedulerSaveReqVO schedulerSaveReqVO = new CollectorEtlSchedulerSaveReqVO();
                    schedulerSaveReqVO.setId(schedulerDO.getId());
                    schedulerSaveReqVO.setStatus("0");
                    iCollectorEtlSchedulerService.updateCollectorEtlScheduler(schedulerSaveReqVO);
                }
                log.warn("任务已发布，但调度器上线失败，taskId={}，taskCode={}，原因：{}",
                        taskDO.getId(), taskCode, e.getMessage(), e);
            }
        }

        return BeanUtils.toBean(taskDO, CollectorEtlTaskSaveReqVO.class);
    }

    /**
     * 发布新版逻辑集成任务。前端的一个 Pipeline Node 在 DS 中对应一个独立的 CHUNJUN 节点，
     * 画布边关系转换成 DS 的 taskRelation；元数据探查和质量检查按配置追加为 HTTP 节点。
     */
    @SuppressWarnings("unchecked")
    private CollectorEtlTaskSaveReqVO publishPipelineTask(CollectorEtlNewNodeSaveReqVO reqVO,
                                                           CollectorEtlTaskDO taskDO) {
        boolean componentWorkflow = isCompiledComponentWorkflow(reqVO);
        if (componentWorkflow) {
            runGuardMapper.lockTask(taskDO.getId());
            componentRecovery.cancelPending(taskDO.getId(), "任务重新发布，取消旧版本恢复");
        }
        CollectorEtlTaskExtDO taskExt = CollectorEtlTaskExtService.getByTaskId(taskDO.getId());
        if (runGuardEnabled && componentWorkflow && !runGuardMapper.findActive(taskDO.getId()).isEmpty()) {
            throw new ServiceException("任务仍有活动或待核对运行，不能覆盖其已发布阶段快照，请先等待运行确认结束");
        }
        ProcessDefinition existing = componentWorkflow ? null
                : dsEtlTaskService.getTaskByName(String.valueOf(reqVO.getSpaceCode()), reqVO.getName());
        boolean firstPublish = taskExt == null || StringUtils.isBlank(taskExt.getEtlTaskCode());
        String taskCode;
        if (!firstPublish) {
            taskCode = taskExt.getEtlTaskCode();
        } else if (existing != null && StringUtils.isNotBlank(existing.getCode())) {
            taskCode = existing.getCode();
        } else {
            taskCode = String.valueOf(dsEtlNodeService.genCode(reqVO.getSpaceCode()).getData().get(0));
        }

        String primaryCode = taskExt == null ? null : taskExt.getEtlNodeCode();
        if (StringUtils.isBlank(primaryCode)) {
            primaryCode = nextDsNodeCode(reqVO.getSpaceCode());
        }
        Map<String, Object> taskInfo = new HashMap<>();
        taskInfo.put("spaceCode", reqVO.getSpaceCode());
        taskInfo.put("taskCode", taskCode);
        taskInfo.put("taskVersion", firstPublish ? 1 : (taskExt.getEtlTaskVersion() == null ? 1 : taskExt.getEtlTaskVersion() + 1));
        taskInfo.put("name", reqVO.getName());

        String callbackToken = componentWorkflow && runGuardEnabled ? CollectorEtlCallbackTokenService.generate() : null;
        PipelinePublishPayload payload = buildPipelinePublishPayload(reqVO, taskInfo, primaryCode, taskExt, callbackToken);
        DsTaskSaveReqDTO saveReq = new DsTaskSaveReqDTO();
        saveReq.setName(reqVO.getName());
        saveReq.setDescription(reqVO.getDescription());
        saveReq.setExecutionType(StringUtils.isNotBlank(reqVO.getExecutionType()) ? reqVO.getExecutionType() : "SERIAL_WAIT");
        saveReq.setTaskDefinitionJson(JSON.toJSONString(payload.taskDefinitions));
        saveReq.setTaskRelationJson(JSON.toJSONString(payload.taskRelations));
        saveReq.setLocations(JSON.toJSONString(payload.locations));

        DsTaskSaveRespDTO saveResp = firstPublish && (existing == null || StringUtils.isBlank(existing.getCode()))
                ? dsEtlTaskService.createTask(saveReq, reqVO.getSpaceCode())
                : updateDsTaskAllowingOnline(saveReq, String.valueOf(reqVO.getSpaceCode()), taskCode);
        if (saveResp == null || !saveResp.getSuccess()) {
            throw new ServiceException("发布逻辑集成任务错误:" + (saveResp == null ? "DS 无响应"
                    : componentWorkflow ? "DS 拒绝保存流程定义，请核对任务配置" : saveResp.getMsg()));
        }
        ProcessDefinition definition = saveResp.getData();
        if (definition == null) {
            throw new ServiceException("发布逻辑集成任务错误:DS 未返回流程定义");
        }
        taskCode = definition.getCode();

        TaskDefinition primaryDefinition = getDsTaskDefinition(definition, payload.primaryCode);
        if (primaryDefinition == null && definition.getTaskDefinitionList() != null && !definition.getTaskDefinitionList().isEmpty()) {
            primaryDefinition = definition.getTaskDefinitionList().get(0);
        }
        ProcessTaskRelation primaryRelation = getDsTaskRelation(definition, payload.primaryCode);
        CollectorEtlTaskExtSaveReqVO extSave = CollectorEtlTaskExtSaveReqVO.builder()
                .taskId(taskDO.getId())
                .etlTaskCode(taskCode)
                .etlTaskVersion(definition.getVersion())
                .etlNodeId(primaryDefinition == null ? null : primaryDefinition.getId())
                .etlNodeName(payload.primaryName)
                .etlNodeCode(payload.primaryCode)
                .etlNodeVersion(primaryDefinition == null ? 0 : primaryDefinition.getVersion())
                .etlRelationId(primaryRelation == null ? null : primaryRelation.getId())
                .build();
        if (taskExt == null) {
            CollectorEtlTaskExtService.createCollectorEtlTaskExt(extSave);
        } else {
            copyPublishedExt(extSave, taskExt);
            clearIncrementalExt(taskExt);
            taskExt.setEtlTaskCode(taskCode);
            taskExt.setEtlTaskVersion(definition.getVersion());
            taskExt.setEtlNodeId(extSave.getEtlNodeId());
            taskExt.setEtlNodeName(extSave.getEtlNodeName());
            taskExt.setEtlNodeCode(extSave.getEtlNodeCode());
            taskExt.setEtlNodeVersion(extSave.getEtlNodeVersion());
            taskExt.setEtlRelationId(extSave.getEtlRelationId());
            CollectorEtlTaskExtService.updateById(taskExt);
        }
        if (componentWorkflow) {
            Map<String, Object> runtime = new LinkedHashMap<>();
            runtime.put("kind", CollectorEtlStageRuntimeService.KIND); runtime.put("workflowCode", taskCode);
            runtime.put("workflowVersion", definition.getVersion()); runtime.put("stages", payload.runtimeStages);
            runtime.put("resourcesJson", payload.resourcesJson); runtime.put("guardNodeCode", payload.guardNodeCode);
            if (callbackToken != null) runtime.put("callbackTokenHash", CollectorEtlCallbackTokenService.hash(callbackToken));
            CollectorEtlTaskExtDO savedExt = CollectorEtlTaskExtService.getByTaskId(taskDO.getId());
            savedExt.setFlinkxJobTemplateJson(JSON.toJSONString(runtime));
            CollectorEtlTaskExtService.updateById(savedExt);
        }
        createTaskLogSnapshotIfAbsent(reqVO, definition, taskCode,
                JSON.toJSONString(payload.locations), taskDO.getDsId());

        DsStatusRespDTO releaseResp = dsEtlTaskService.releaseTask("ONLINE", String.valueOf(reqVO.getSpaceCode()), taskCode);
        if (!isDsStatusSuccess(releaseResp)) {
            throw new ServiceException("上线逻辑集成任务失败！");
        }
        updateTaskStatus(taskDO.getId(), "1");
        taskDO.setStatus("1");
        taskDO.setCode(taskCode);
        writeTaskLineageSilently(taskDO, payload.lineageArgs);
        publishPipelineScheduler(taskDO, reqVO, taskCode);
        return BeanUtils.toBean(taskDO, CollectorEtlTaskSaveReqVO.class);
    }

    @SuppressWarnings("unchecked")
    private PipelinePublishPayload buildPipelinePublishPayload(CollectorEtlNewNodeSaveReqVO reqVO,
                                                                Map<String, Object> taskInfo,
                                                                String preferredPrimaryCode,
                                                                CollectorEtlTaskExtDO existingExt, String callbackToken) {
        List<CollectorEtlNodeSaveReqVO> configuredNodes = JSON.parseArray(reqVO.getTaskDefinitionList(), CollectorEtlNodeSaveReqVO.class);
        if (configuredNodes == null || configuredNodes.isEmpty()) {
            throw new ServiceException("逻辑集成任务没有可发布节点");
        }
        List<CollectorEtlNodeSaveReqVO> pipelineNodes = configuredNodes.stream()
                .filter(node -> node != null && TaskComponentTypeEnum.PIPELINE_NODE.getCode().equals(node.getComponentType()))
                .collect(Collectors.toList());
        if (pipelineNodes.isEmpty()) {
            throw new ServiceException("逻辑集成任务没有可发布节点");
        }
        if (pipelineNodes.size() != configuredNodes.size()) {
            throw new ServiceException("逻辑集成节点任务不能与旧版输入/转换/输出节点混用，请拆分任务后再发布");
        }

        Map<String, Object> draft = StringUtils.isBlank(reqVO.getDraftJson())
                ? new HashMap<>() : JSON.parseObject(reqVO.getDraftJson(), Map.class);
        Map<String, Object> pipelineGraph = draft.get("pipelineGraph") instanceof Map
                ? (Map<String, Object>) draft.get("pipelineGraph") : Collections.emptyMap();
        Map<String, String> nodeIdToConfiguredCode = new HashMap<>();
        Object graphNodes = pipelineGraph.get("nodes");
        if (graphNodes instanceof List) {
            for (Object item : (List<?>) graphNodes) {
                if (!(item instanceof Map)) continue;
                Map<String, Object> map = (Map<String, Object>) item;
                String nodeId = configText(map.get("nodeId"));
                String nodeCode = configText(map.get("nodeCode"));
                if (StringUtils.isNotBlank(nodeId) && StringUtils.isNotBlank(nodeCode)) {
                    // pipelineNodeId 是首选标识；nodeCode 作为旧草稿兼容别名。
                    nodeIdToConfiguredCode.put(nodeId, nodeId);
                    nodeIdToConfiguredCode.put(nodeCode, nodeId);
                }
            }
        }

        List<Map<String, Object>> definitions = new ArrayList<>();
        List<Map<String, Object>> relations = new ArrayList<>();
        List<Map<String, Object>> locations = new ArrayList<>();
        Map<String, String> configuredCodeToDsCode = new HashMap<>();
        Map<String, String> beforeNode = new HashMap<>();
        Map<String, String> prepareTail = new HashMap<>();
        Map<String, Object> runtimeStages = new LinkedHashMap<>();
        Map<String, String> afterNode = new HashMap<>();
        Map<String, Map<String, Object>> pipelineConfigByCode = new HashMap<>();
        for (CollectorEtlNodeSaveReqVO node : pipelineNodes) {
            String configuredCode = resolvePipelineNodeKey(node);
            if (StringUtils.isBlank(configuredCode)) {
                throw new ServiceException("逻辑集成节点缺少稳定节点标识");
            }
            if (pipelineConfigByCode.containsKey(configuredCode)) {
                throw new ServiceException("逻辑集成节点标识重复: " + configuredCode);
            }
            if (node.getTaskParams() != null && node.getTaskParams().get("pipelineNode") instanceof Map) {
                Map<String, Object> pipeline = new LinkedHashMap<>(
                        (Map<String, Object>) node.getTaskParams().get("pipelineNode"));
                ensurePipelineSourceMetadata(pipeline);
                if (StringUtils.isNotBlank(node.getCode())) {
                    pipeline.putIfAbsent("nodeCode", node.getCode());
                }
                normalizePipelineConfig(pipeline);
                if (!"CONDITION".equals(String.valueOf(pipeline.get("mode")))) {
                    validatePipelineSourceMetadataConfig(pipeline, node.getName());
                    validatePipelineAuxiliaryConfig(pipeline, node.getName());
                }
                pipelineConfigByCode.put(configuredCode, pipeline);
            }
        }
        validatePipelineDependencies(pipelineGraph, nodeIdToConfiguredCode, pipelineConfigByCode);
        List<Map<String, Object>> publishedLineageUnits = new ArrayList<>();
        String primaryCode = null;
        String primaryName = null;
        int index = 0;

        int codeIndex = 0;
        for (CollectorEtlNodeSaveReqVO node : pipelineNodes) {
            configuredCodeToDsCode.put(resolvePipelineNodeKey(node), codeIndex++ == 0 && StringUtils.isNotBlank(preferredPrimaryCode)
                    ? preferredPrimaryCode : nextDsNodeCode(reqVO.getSpaceCode()));
        }
        for (CollectorEtlNodeSaveReqVO node : pipelineNodes) {
            String configuredCode = resolvePipelineNodeKey(node);
            if (StringUtils.isBlank(configuredCode)) {
                throw new ServiceException("逻辑集成节点缺少稳定节点标识");
            }
            String dsCode = configuredCodeToDsCode.get(configuredCode);
            configuredCodeToDsCode.put(configuredCode, dsCode);
            if (index == 0) {
                primaryCode = dsCode;
                primaryName = node.getName();
            }

            Map<String, Object> perNodeInfo = new HashMap<>(taskInfo);
            perNodeInfo.put("name", node.getName());
            Map<String, Object> controlConfig = pipelineConfigByCode.get(configuredCode);
            if ("CONDITION".equals(String.valueOf(controlConfig.get("mode")))) {
                Map<String, Object> control = JSON.parseArray(TaskConverter.buildEtlTaskDefinitionJsonFlinkx(
                        null, node.getName(), dsCode, 0, "{}", reqVO.getDraftJson(), getProjectWorkerGroup(reqVO.getSpaceCode())), Map.class).get(0);
                control.put("taskType", "CONDITIONS");
                control.remove("taskGroupId"); control.remove("taskGroupPriority");
                Map<String, Object> parameters = new LinkedHashMap<>();
                parameters.put("localParams", new ArrayList<>()); parameters.put("resourceList", new ArrayList<>());
                List<Map<String, Object>> items = new ArrayList<>();
                for (Object ref : (List<?>) controlConfig.get("controlDependencies")) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("depTaskCode", Long.valueOf(configuredCodeToDsCode.get(String.valueOf(ref))));
                    item.put("status", "SUCCESS"); items.add(item);
                }
                Map<String, Object> group = new LinkedHashMap<>();
                group.put("relation", controlConfig.get("conditionRelation")); group.put("dependItemList", items);
                Map<String, Object> dependence = new LinkedHashMap<>();
                dependence.put("relation", "AND"); dependence.put("dependTaskList", Collections.singletonList(group));
                parameters.put("dependence", dependence);
                Map<String, Object> conditionResult = new LinkedHashMap<>();
                conditionResult.put("conditionSuccess", true);
                for (String branch : Arrays.asList("successStages", "failedStages")) {
                    List<Long> targets = new ArrayList<>();
                    for (Object ref : (List<?>) controlConfig.get(branch)) targets.add(Long.valueOf(configuredCodeToDsCode.get(String.valueOf(ref))));
                    conditionResult.put("successStages".equals(branch) ? "successNode" : "failedNode", targets);
                }
                parameters.put("conditionResult", conditionResult); control.put("taskParams", parameters);
                definitions.add(control);
                Map<String, Object> position = new LinkedHashMap<>();
                position.put("taskCode", Long.valueOf(dsCode)); position.put("x", 120 + index * 260); position.put("y", 120);
                locations.add(position); index++;
                continue;
            }
            preparePipelineInputs(node, configuredCode, pipelineConfigByCode);
            Map<String, Object> preparedConfig = pipelineConfigByCode.get(configuredCode);
            if (!hasConfiguredPipelineColumns(preparedConfig)) {
                throw new ServiceException("逻辑节点“" + node.getName() + "”未配置源字段，请先读取源表字段");
            }
            validatePipelineDataSources(preparedConfig, node.getName());
            validatePipelineWriteMode(preparedConfig, node.getName());
            Map<String, Object> args = TaskConverter.buildEtlTaskParams(
                    JSON.toJSONString(Collections.singletonList(node)), new HashMap<>(), perNodeInfo, new ArrayList<>());
            Map<String, Object> parsedPipeline = null;
            Object pipelineList = args.get("pipelineNodes");
            if (pipelineList instanceof List && !((List<?>) pipelineList).isEmpty()
                    && ((List<?>) pipelineList).get(0) instanceof Map) {
                parsedPipeline = (Map<String, Object>) ((List<?>) pipelineList).get(0);
            }
            if (parsedPipeline == null || !(args.get("reader") instanceof Map) || !(args.get("writer") instanceof Map)) {
                throw new ServiceException("逻辑节点“" + node.getName() + "”缺少完整的输入/输出数据源配置");
            }
            Map<String, Object> config = parsedPipeline.get("pipelineNode") instanceof Map
                    ? (Map<String, Object>) parsedPipeline.get("pipelineNode") : Collections.emptyMap();
            if (parsedPipeline.get("pipelineNode") instanceof Map) {
                config.putIfAbsent("pipelineNodeId", configuredCode);
                if (StringUtils.isNotBlank(node.getCode())) config.putIfAbsent("nodeCode", node.getCode());
            }
            normalizePipelineConfig(config);
            validatePipelineSourceMetadataConfig(config, node.getName());
            validatePipelineAuxiliaryConfig(config, node.getName());
            pipelineConfigByCode.put(configuredCode, config);

            if (!hasConfiguredPipelineColumns(config)) {
                throw new ServiceException("逻辑节点“" + node.getName() + "”未配置源字段，请先读取源表字段");
            }
            validatePipelineDataSources(config, node.getName());
            validatePipelineTargetGovernanceColumns(config, node.getName());
            validatePipelineWriteMode(config, node.getName());

            if ("MERGE".equalsIgnoreCase(String.valueOf(config.get("mode")))) {
                validatePipelineMergeInputs(config, node.getName(), configuredCode);
            }

            Object componentTransforms = config.get("transforms");
            if (componentTransforms instanceof List && !((List<?>) componentTransforms).isEmpty()) {
                List<CollectorEtlNodeSaveReqVO> transforms = JSON.parseArray(
                        JSON.toJSONString(componentTransforms), CollectorEtlNodeSaveReqVO.class);
                Map<String, Object> transformArgs = TaskConverter.buildEtlTaskParams(
                        JSON.toJSONString(transforms), new HashMap<>(), perNodeInfo, new ArrayList<>());
                args.put("transition", transformArgs.get("transition"));
            }
            if (StringUtils.isNotBlank(configText(config.get("rowFilter")))) {
                args.put("rowFilter", config.get("rowFilter"));
            }
            perNodeInfo.put("componentIds", config.get("componentIds"));
            args.put("componentStage", config.containsKey("componentIds"));
            String flinkxJobJson = FlinkxEtlTaskConverter.convertToFlinkxJobJson(args);
            Map<String, Object> definition = JSON.parseArray(
                    TaskConverter.buildEtlTaskDefinitionJsonFlinkx(null, node.getName(), dsCode, 0,
                            flinkxJobJson, reqVO.getDraftJson(), getProjectWorkerGroup(reqVO.getSpaceCode())),
                    Map.class).get(0);
            if (index == 0 && existingExt != null) {
                definition.put("id", existingExt.getEtlNodeId());
                definition.put("version", existingExt.getEtlNodeVersion() == null ? 0 : existingExt.getEtlNodeVersion());
            }
            definitions.add(definition);
            args.put("publishedJobJson", flinkxJobJson);
            Map<String, Object> lineageUnit = new HashMap<>();
            lineageUnit.put("unitKey", configuredCode);
            lineageUnit.put("args", args);
            publishedLineageUnits.add(lineageUnit);
            Map<String, Object> position = new HashMap<>();
            position.put("taskCode", Long.parseLong(dsCode));
            position.put("x", 120 + index * 260);
            position.put("y", 120 + (index % 4) * 160);
            locations.add(position);

            addPipelineAuxiliaryNodes(config, node.getName(), configuredCode, reqVO, definitions, locations,
                    beforeNode, afterNode, index);
            Map<String, Object> incremental = config.get("incremental") instanceof Map
                    ? (Map<String, Object>) config.get("incremental") : Collections.emptyMap();
            if (isCompiledComponentWorkflow(reqVO) && incremental.get("sources") instanceof List
                    && !((List<?>) incremental.get("sources")).isEmpty()) {
                if (!stageIncrementalEnabled || !runGuardEnabled) throw new ServiceException("组件增量需要先启用阶段窗口和大任务运行保护");
                if (StringUtils.isBlank(stagePrepareUrl) || callbackToken == null) {
                    throw new ServiceException("阶段增量需配置 DS 可访问的 stage_prepare_url 并生成任务回调凭据");
                }
                validateIncrementalStagePrimaryKey(config, node.getName());
                String prepareCode = nextDsNodeCode(reqVO.getSpaceCode());
                String prepareName = "增量准备-" + prepareCode;
                Map<String, Object> prepare = TaskConverter.buildHttpTaskDefinitionMap(null, prepareName, prepareCode, 0,
                        stagePrepareUrl.replaceAll("/+$", "") + "/" + resolveTaskId(reqVO), reqVO.getDraftJson(), getProjectWorkerGroup(reqVO.getSpaceCode()));
                Map<String, Object> http = (Map<String, Object>) prepare.get("taskParams");
                List<Map<String, Object>> parameters = new ArrayList<>();
                parameters.add(httpParameter("executionId", "PARAMETER", ""));
                parameters.add(httpParameter("stageKey", "PARAMETER", configuredCode));
                parameters.add(httpParameter(CollectorEtlCallbackTokenService.HEADER, "HEADERS", callbackToken));
                http.put("httpParams", parameters); http.put("httpCheckCondition", "STATUS_CODE_DEFAULT");
                prepare.put("failRetryTimes", 0); definitions.add(prepare);
                String previous = beforeNode.get(configuredCode);
                if (previous == null) beforeNode.put(configuredCode, prepareCode);
                else {
                    Map<String, Object> relation = new LinkedHashMap<>();
                    relation.put("preTaskCode", Long.valueOf(previous)); relation.put("preTaskVersion", 0);
                    relation.put("postTaskCode", Long.valueOf(prepareCode)); relation.put("postTaskVersion", 0);
                    relation.put("conditionType", "NONE"); relations.add(relation);
                }
                prepareTail.put(configuredCode, prepareCode);
                ((Map<String, Object>) definition.get("taskParams")).put("json", "${" + prepareName + ".response}");
                Map<String, Object> runtime = new LinkedHashMap<>();
                runtime.put("config", config); runtime.put("args", args); runtime.put("dsNodeCode", dsCode);
                runtime.put("prepareNodeCode", prepareCode);
                runtime.put("configHash", com.datamaster.module.collector.utils.StageRuntimeFingerprint.of(flinkxJobJson, incremental));
                runtimeStages.put(configuredCode, JSON.parseObject(JSON.toJSONString(runtime), Map.class));
            }
            index++;
        }

        // 旧草稿可能没有 pipelineNodeId：若 graph.nodeCode 能对应任务 code，则把画布 nodeId
        // 映射回旧 code；新草稿则自然映射到 pipelineNodeId。
        if (graphNodes instanceof List) {
            for (Object item : (List<?>) graphNodes) {
                if (!(item instanceof Map)) continue;
                Map<String, Object> map = (Map<String, Object>) item;
                String nodeId = configText(map.get("nodeId"));
                String nodeCode = configText(map.get("nodeCode"));
                if (StringUtils.isNotBlank(nodeId) && StringUtils.isNotBlank(nodeCode)) {
                    nodeIdToConfiguredCode.put(nodeId,
                            configuredCodeToDsCode.containsKey(nodeCode) ? nodeCode : nodeId);
                }
            }
        }

        // 条件节点选择的是实际分支入口；有阶段prepare时不能仍指向CHUNJUN正文。
        if (isCompiledComponentWorkflow(reqVO)) {
            Map<String, String> branchEntries = new HashMap<>();
            for (Map.Entry<String, String> node : configuredCodeToDsCode.entrySet()) {
                branchEntries.put(node.getValue(), beforeNode.getOrDefault(node.getKey(), node.getValue()));
            }
            for (Map<String, Object> definition : definitions) {
                if (!"CONDITIONS".equals(definition.get("taskType"))) continue;
                Map<String, Object> params = (Map<String, Object>) definition.get("taskParams");
                Map<String, Object> condition = (Map<String, Object>) params.get("conditionResult");
                for (String branch : Arrays.asList("successNode", "failedNode")) {
                    List<Long> entries = new ArrayList<>();
                    for (Object target : (List<?>) condition.get(branch)) {
                        String code = String.valueOf(target);
                        entries.add(Long.valueOf(branchEntries.getOrDefault(code, code)));
                    }
                    condition.put(branch, entries);
                }
            }
        }
        Object edges = pipelineGraph.get("edges");
        Set<String> relationKeys = new HashSet<>();
        Set<String> incoming = new HashSet<>();
        if (edges instanceof List) {
            for (Object item : (List<?>) edges) {
                if (!(item instanceof Map)) continue;
                Map<String, Object> edge = (Map<String, Object>) item;
                String sourceConfigured = resolveConfiguredCode(configText(edge.get("source")), nodeIdToConfiguredCode);
                String targetConfigured = resolveConfiguredCode(configText(edge.get("target")), nodeIdToConfiguredCode);
                sourceConfigured = resolvePipelineConfigKey(sourceConfigured, pipelineConfigByCode);
                targetConfigured = resolvePipelineConfigKey(targetConfigured, pipelineConfigByCode);
                String sourceDs = configuredCodeToDsCode.get(sourceConfigured);
                String targetDs = configuredCodeToDsCode.get(targetConfigured);
                if (StringUtils.isBlank(sourceDs) || StringUtils.isBlank(targetDs)) continue;
                incoming.add(targetConfigured);
                String pre = afterNode.getOrDefault(sourceConfigured, sourceDs);
                String post = beforeNode.getOrDefault(targetConfigured, targetDs);
                addPipelineRelation(relations, relationKeys, pre, post);
            }
        }
        // 兼容没有保存 pipelineGraph.edges 的旧草稿：以节点输入中的 refNodeId/refNodeCode 补建依赖。
        for (Map.Entry<String, Map<String, Object>> entry : pipelineConfigByCode.entrySet()) {
            Object inputs = entry.getValue() == null ? null : entry.getValue().get("inputs");
            if (!(inputs instanceof List)) continue;
            String targetDs = configuredCodeToDsCode.get(entry.getKey());
            if (StringUtils.isBlank(targetDs)) continue;
            for (Object input : (List<?>) inputs) {
                if (!(input instanceof Map)) continue;
                Map<String, Object> inputMap = (Map<String, Object>) input;
                String ref = firstConfigValue(inputMap, "refNodeCode", "refNodeId");
                ref = resolveConfiguredCode(ref, nodeIdToConfiguredCode);
                ref = resolvePipelineConfigKey(ref, pipelineConfigByCode);
                String sourceDs = configuredCodeToDsCode.get(ref);
                if (StringUtils.isBlank(sourceDs)) continue;
                incoming.add(entry.getKey());
                String pre = afterNode.getOrDefault(ref, sourceDs);
                String post = beforeNode.getOrDefault(entry.getKey(), targetDs);
                addPipelineRelation(relations, relationKeys, pre, post);
            }
        }
        for (String configuredCode : configuredCodeToDsCode.keySet()) {
            String nodeDs = configuredCodeToDsCode.get(configuredCode);
            String first = beforeNode.getOrDefault(configuredCode, nodeDs);
            if (!incoming.contains(configuredCode)) {
                addPipelineRelation(relations, relationKeys, "0", first);
            }
            if (beforeNode.containsKey(configuredCode)) {
                addPipelineRelation(relations, relationKeys, prepareTail.getOrDefault(configuredCode, beforeNode.get(configuredCode)), nodeDs);
            }
            if (afterNode.containsKey(configuredCode)) {
                addPipelineRelation(relations, relationKeys, nodeDs, afterNode.get(configuredCode));
            }
        }
        if (existingExt != null && existingExt.getEtlRelationId() != null) {
            for (Map<String, Object> relation : relations) {
                if (String.valueOf(relation.get("postTaskCode")).equals(primaryCode)) {
                    relation.put("id", existingExt.getEtlRelationId());
                    break;
                }
            }
        }

        Map<String, Object> guardContext = Collections.emptyMap();
        if (runGuardEnabled && isCompiledComponentWorkflow(reqVO)) {
            guardContext = addWorkflowRunGuard(reqVO, definitions, relations, locations, callbackToken);
        }
        Map<String, Object> lineageArgs = new HashMap<>();
        lineageArgs.put("lineageUnits", publishedLineageUnits);
        PipelinePublishPayload result = new PipelinePublishPayload(definitions, relations, locations, primaryCode, primaryName, lineageArgs);
        result.runtimeStages = runtimeStages;
        result.resourcesJson = (String) guardContext.get("resourcesJson");
        result.guardNodeCode = (String) guardContext.get("guardNodeCode");
        return result;
    }

    @SuppressWarnings("unchecked")
    private void validateIncrementalStagePrimaryKey(Map<String, Object> config, String nodeName) {
        Map<String, Object> output = (Map<String, Object>) config.get("output");
        Map<String, Object> datasource = (Map<String, Object>) output.get("writerDatasource");
        Long id = toLong(firstNonNull(datasource.get("datasourceId"), datasource.get("id"), null));
        List<DbColumn> physical = datasourceApiService.getDbTableColumns(id, firstConfigValue(output, "table", "target_table_name"));
        Set<String> primary = new LinkedHashSet<>();
        for (DbColumn column : physical) if (Boolean.TRUE.equals(column.getColKey())) primary.add(column.getColName());
        Set<String> selected = new LinkedHashSet<>(normalizePipelineColumnNames(output.get("selectedColumns")));
        Set<String> mapped = new LinkedHashSet<>(normalizePipelineColumnNames(output.get("target_columns")));
        if (primary.isEmpty() || !primary.equals(selected) || !mapped.containsAll(primary)) {
            throw new ServiceException("增量阶段“" + nodeName + "”的更新键必须覆盖实际目标表主键并包含于字段映射；唯一索引场景暂不自动认定");
        }
    }

    private Map<String, Object> httpParameter(String name, String type, String value) {
        Map<String, Object> parameter = new LinkedHashMap<>(); parameter.put("prop", name);
        parameter.put("httpParametersType", type); parameter.put("value", value); return parameter;
    }

    private boolean isComponentSnapshot(CollectorEtlTaskExtDO ext) {
        return ext != null && StringUtils.isNotBlank(ext.getFlinkxJobTemplateJson())
                && CollectorEtlStageRuntimeService.KIND.equals(JSON.parseObject(ext.getFlinkxJobTemplateJson()).getString("kind"));
    }

    private boolean isCompiledComponentWorkflow(CollectorEtlNewNodeSaveReqVO request) {
        return StringUtils.isNotBlank(request.getDraftJson())
                && JSON.parseObject(request.getDraftJson()).get("componentSnapshot") instanceof Map;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> addWorkflowRunGuard(CollectorEtlNewNodeSaveReqVO request, List<Map<String, Object>> definitions,
                                     List<Map<String, Object>> relations, List<Map<String, Object>> locations) {
        return Collections.emptyMap();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> addWorkflowRunGuard(CollectorEtlNewNodeSaveReqVO request, List<Map<String, Object>> definitions,
                                     List<Map<String, Object>> relations, List<Map<String, Object>> locations, String callbackToken) {
        if (!isCompiledComponentWorkflow(request)) return Collections.emptyMap();
        if (StringUtils.isBlank(runGuardPrepareUrl)) throw new ServiceException("启用运行保护必须配置 DS 可访问的 prepare_url");
        if (callbackToken == null) throw new ServiceException("任务回调凭据尚未生成");
        // 只保护大任务自身，不申请跨任务目标表独占。
        Long taskId = resolveTaskId(request);
        String resources = "[]";
        String code = nextDsNodeCode(request.getSpaceCode());
        String guardName = "工作流运行准入-" + code;
        Map<String, Object> guard = TaskConverter.buildHttpTaskDefinitionMap(null, guardName, code, 0,
                runGuardPrepareUrl.replaceAll("/+$", "") + "/" + taskId, request.getDraftJson(), getProjectWorkerGroup(request.getSpaceCode()));
        Map<String, Object> params = (Map<String, Object>) guard.get("taskParams");
        params.put("httpMethod", "POST");
        params.put("httpBody", JSON.toJSONString(Collections.singletonMap("resourcesJson", resources)));
        List<Map<String, Object>> httpParams = new ArrayList<>();
        httpParams.add(httpParameter(CollectorEtlCallbackTokenService.HEADER, "HEADERS", callbackToken));
        Map<String, Object> contentType = new LinkedHashMap<>(); contentType.put("prop", "Content-Type");
        contentType.put("httpParametersType", "HEADERS"); contentType.put("value", "application/json"); httpParams.add(contentType);
        params.put("httpParams", httpParams);
        params.put("httpCheckCondition", "STATUS_CODE_DEFAULT");
        for (Map<String, Object> definition : definitions) {
            if (!(definition.get("taskParams") instanceof Map)) continue;
            Map<String, Object> stageParams = (Map<String, Object>) definition.get("taskParams");
            if (!(stageParams.get("httpParams") instanceof List)) continue;
            for (Map<String, Object> parameter : (List<Map<String, Object>>) stageParams.get("httpParams")) {
                if ("executionId".equals(parameter.get("prop"))) parameter.put("value", "${" + guardName + ".response}");
            }
        }
        guard.put("failRetryTimes", 0);
        Set<String> seen = new HashSet<>();
        for (Map<String, Object> relation : relations) {
            if ("0".equals(String.valueOf(relation.get("preTaskCode")))) relation.put("preTaskCode", Long.valueOf(code));
        }
        addPipelineRelation(relations, seen, "0", code);
        definitions.add(guard);
        Map<String, Object> position = new LinkedHashMap<>(); position.put("taskCode", Long.valueOf(code)); position.put("x", 0); position.put("y", 0);
        locations.add(position);
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("resourcesJson", resources); context.put("guardNodeCode", code);
        return context;
    }

    @SuppressWarnings("unchecked")
    private boolean hasConfiguredPipelineColumns(Map<String, Object> config) {
        if (config == null) return false;
        Object columns = config.get("columns");
        if (!(columns instanceof List) || ((List<?>) columns).isEmpty()) {
            Map<String, Object> input = config.get("input") instanceof Map
                    ? (Map<String, Object>) config.get("input") : Collections.emptyMap();
            columns = input.get("columns");
        }
        return columns instanceof List && !((List<?>) columns).isEmpty();
    }

    @SuppressWarnings("unchecked")
    private void ensurePipelineSourceMetadata(Map<String, Object> pipeline) {
        if (pipeline == null) return;
        Map<String, Object> metadata = pipeline.get("sourceMetadata") instanceof Map
                ? new LinkedHashMap<>((Map<String, Object>) pipeline.get("sourceMetadata"))
                : new LinkedHashMap<>();
        if (metadata.isEmpty() && pipeline.get("input") instanceof Map
                && ((Map<?, ?>) pipeline.get("input")).get("sourceMetadata") instanceof Map) {
            metadata.putAll((Map<String, Object>) ((Map<?, ?>) pipeline.get("input")).get("sourceMetadata"));
        }
        // 逻辑集成节点的 source_table / write_time 是不可关闭的治理契约，
        // 这里同时兼容早期没有 sourceMetadata 配置的草稿。
        metadata.put("enabled", true);
        pipeline.put("sourceMetadata", metadata);
    }

    /**
     * 兼容旧草稿中把逗号分隔字符串保存到 selectedColumns / naturalKeyColumns 的情况。
     * 发布器内部统一使用 List，避免校验通过但 FlinkX writer 丢失更新主键。
     */
    @SuppressWarnings("unchecked")
    private void normalizePipelineConfig(Map<String, Object> pipeline) {
        if (pipeline == null) return;
        if (StringUtils.isBlank(configText(pipeline.get("mode")))) {
            pipeline.put("mode", "INTEGRATION");
        }
        if (pipeline.containsKey("columns")) {
            pipeline.put("columns", normalizePipelineColumnList(pipeline.get("columns")));
        }
        if (pipeline.containsKey("targetColumns")) {
            pipeline.put("targetColumns", normalizePipelineColumnList(pipeline.get("targetColumns")));
        }
        Map<String, Object> input = pipeline.get("input") instanceof Map
                ? (Map<String, Object>) pipeline.get("input") : null;
        if (input != null) {
            if (input.containsKey("columns")) {
                input.put("columns", normalizePipelineColumnList(input.get("columns")));
            }
        }
        Map<String, Object> output = pipeline.get("output") instanceof Map
                ? (Map<String, Object>) pipeline.get("output") : null;
        if (output != null) {
            if (output.containsKey("columns")) {
                output.put("columns", normalizePipelineColumnList(output.get("columns")));
            }
            if (output.containsKey("target_columns")) {
                output.put("target_columns", normalizePipelineColumnList(output.get("target_columns")));
            }
            output.put("selectedColumns", normalizeStringList(output.get("selectedColumns"),
                    output.get("selectedColumnsText")));
        }
        Map<String, Object> metadata = pipeline.get("sourceMetadata") instanceof Map
                ? (Map<String, Object>) pipeline.get("sourceMetadata") : null;
        if (metadata != null) {
            metadata.put("naturalKeyColumns", normalizeStringList(metadata.get("naturalKeyColumns"),
                    metadata.get("naturalKeyColumnsText")));
        }
    }

    private List<String> normalizeStringList(Object primary, Object fallback) {
        Object raw = primary;
        if (raw == null || (raw instanceof String && StringUtils.isBlank((String) raw))) raw = fallback;
        List<String> result = new ArrayList<>();
        if (raw instanceof Collection) {
            for (Object item : (Collection<?>) raw) {
                if (item != null && StringUtils.isNotBlank(String.valueOf(item))) {
                    result.add(String.valueOf(item).trim());
                }
            }
            return result;
        }
        if (raw != null) {
            for (String item : String.valueOf(raw).split(",")) {
                if (StringUtils.isNotBlank(item)) result.add(item.trim());
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<String> normalizePipelineColumnList(Object raw) {
        List<String> result = new ArrayList<>();
        if (raw instanceof Collection) {
            for (Object item : (Collection<?>) raw) {
                if (item instanceof Map) {
                    Map<String, Object> field = (Map<String, Object>) item;
                    String name = configText(field.get("columnName"));
                    if (StringUtils.isBlank(name)) name = configText(field.get("name"));
                    if (StringUtils.isNotBlank(name) && !result.contains(name)) result.add(name);
                } else if (item != null && StringUtils.isNotBlank(String.valueOf(item))) {
                    String name = String.valueOf(item).trim();
                    if (!result.contains(name)) result.add(name);
                }
            }
            return result;
        }
        if (raw != null && StringUtils.isNotBlank(String.valueOf(raw))) {
            for (String item : String.valueOf(raw).split(",")) {
                if (StringUtils.isNotBlank(item) && !result.contains(item.trim())) result.add(item.trim());
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private void validatePipelineDependencies(Map<String, Object> pipelineGraph,
                                              Map<String, String> nodeIdToConfiguredCode,
                                              Map<String, Map<String, Object>> pipelineConfigByCode) {
        Map<String, List<String>> adjacency = new HashMap<>();
        if (pipelineGraph != null && pipelineGraph.get("edges") instanceof List) {
            for (Object item : (List<?>) pipelineGraph.get("edges")) {
                if (!(item instanceof Map)) continue;
                Map<String, Object> edge = (Map<String, Object>) item;
                String source = resolveConfiguredCode(configText(edge.get("source")), nodeIdToConfiguredCode);
                String target = resolveConfiguredCode(configText(edge.get("target")), nodeIdToConfiguredCode);
                if (StringUtils.isBlank(source) || StringUtils.isBlank(target)) {
                    throw new ServiceException("逻辑集成节点依赖边必须同时包含 source 和 target");
                }
                source = resolvePipelineConfigKey(source, pipelineConfigByCode);
                target = resolvePipelineConfigKey(target, pipelineConfigByCode);
                addPipelineDependency(adjacency, source, target, pipelineConfigByCode);
            }
        }
        for (Map.Entry<String, Map<String, Object>> entry : pipelineConfigByCode.entrySet()) {
            Object inputs = entry.getValue() == null ? null : entry.getValue().get("inputs");
            if (!(inputs instanceof List)) continue;
            for (Object item : (List<?>) inputs) {
                if (!(item instanceof Map)) continue;
                String ref = firstConfigValue((Map<String, Object>) item, "refNodeCode", "refNodeId");
                if (StringUtils.isBlank(ref)) continue;
                ref = resolveConfiguredCode(ref, nodeIdToConfiguredCode);
                ref = resolvePipelineConfigKey(ref, pipelineConfigByCode);
                addPipelineDependency(adjacency, ref, entry.getKey(), pipelineConfigByCode);
            }
        }
        Map<String, Integer> colors = new HashMap<>();
        for (String node : pipelineConfigByCode.keySet()) {
            if (hasPipelineCycle(node, adjacency, colors)) {
                throw new ServiceException("逻辑集成节点依赖不能形成环，请按上游到下游配置");
            }
        }
    }

    private void addPipelineDependency(Map<String, List<String>> adjacency, String source, String target,
                                       Map<String, Map<String, Object>> pipelineConfigByCode) {
        if (StringUtils.isBlank(source) || StringUtils.isBlank(target)
                || !pipelineConfigByCode.containsKey(source) || !pipelineConfigByCode.containsKey(target)) {
            if (StringUtils.isNotBlank(source) && StringUtils.isNotBlank(target)
                    && (!pipelineConfigByCode.containsKey(source) || !pipelineConfigByCode.containsKey(target))) {
                throw new ServiceException("逻辑集成节点依赖引用了不存在的节点: " + source + " -> " + target);
            }
            return;
        }
        List<String> next = adjacency.computeIfAbsent(source, key -> new ArrayList<>());
        if (!next.contains(target)) next.add(target);
    }

    private boolean hasPipelineCycle(String node, Map<String, List<String>> adjacency,
                                     Map<String, Integer> colors) {
        Integer color = colors.get(node);
        if (color != null) return color == 1;
        colors.put(node, 1);
        for (String next : adjacency.getOrDefault(node, Collections.emptyList())) {
            if (hasPipelineCycle(next, adjacency, colors)) return true;
        }
        colors.put(node, 2);
        return false;
    }

    @SuppressWarnings("unchecked")
    private void validatePipelineWriteMode(Map<String, Object> config, String nodeName) {
        if (config == null) return;
        Map<String, Object> output = config.get("output") instanceof Map
                ? (Map<String, Object>) config.get("output") : Collections.emptyMap();
        String mode = String.valueOf(output.getOrDefault("writeModeType", "2"));
        if (!"3".equals(mode)) return;
        Object selected = output.get("selectedColumns");
        boolean hasKey = selected instanceof List && !((List<?>) selected).isEmpty();
        String dbType = output.get("writerDatasource") instanceof Map
                ? String.valueOf(((Map<String, Object>) output.get("writerDatasource")).get("datasourceType"))
                : "";
        if (!hasKey && !"doris".equalsIgnoreCase(dbType)) {
            throw new ServiceException("逻辑节点“" + nodeName + "”使用增量更新时必须配置更新主键字段");
        }
    }

    /**
     * 辅助治理任务属于逻辑节点发布配置的一部分。启用开关后必须绑定已有任务，
     * 否则不能在发布时静默忽略，避免用户误以为已经执行元数据/质量检查。
     */
    @SuppressWarnings("unchecked")
    private void validatePipelineAuxiliaryConfig(Map<String, Object> config, String nodeName) {
        if (config == null) return;
        Map<String, Object> schema = config.get("schemaMonitoring") instanceof Map
                ? (Map<String, Object>) config.get("schemaMonitoring") : Collections.emptyMap();
        if (schema.isEmpty() && config.get("input") instanceof Map
                && ((Map<?, ?>) config.get("input")).get("schemaMonitoring") instanceof Map) {
            schema = (Map<String, Object>) ((Map<?, ?>) config.get("input")).get("schemaMonitoring");
        }
        if (isConfigEnabled(schema.get("enabled"))
                && StringUtils.isBlank(firstConfigValue(schema, "taskId", "catalogTaskId"))) {
            throw new ServiceException("逻辑节点“" + nodeName + "”已启用元数据探查，但未选择元数据探查任务");
        }
        Map<String, Object> output = config.get("output") instanceof Map
                ? (Map<String, Object>) config.get("output") : Collections.emptyMap();
        Map<String, Object> quality = output.get("qualityConfig") instanceof Map
                ? (Map<String, Object>) output.get("qualityConfig") : Collections.emptyMap();
        if (isConfigEnabled(quality.get("enabled"))
                && StringUtils.isBlank(firstConfigValue(quality, "taskId", "qualityTaskId"))) {
            throw new ServiceException("逻辑节点“" + nodeName + "”已启用质量检查，但未选择质量检查任务");
        }
    }

    @SuppressWarnings("unchecked")
    private void validatePipelineSourceMetadataConfig(Map<String, Object> config, String nodeName) {
        if (config == null) return;
        Map<String, Object> metadata = config.get("sourceMetadata") instanceof Map
                ? (Map<String, Object>) config.get("sourceMetadata") : Collections.emptyMap();
        if (isConfigEnabled(metadata.get("includeSourceUpdateTime"))
                && !SourceMetadataSupport.hasConfiguredSourceColumn(config, "sourceUpdateTimeSourceColumn")) {
            throw new ServiceException("逻辑节点“" + nodeName + "”已启用源端更新时间，但未选择源字段");
        }
        if (isConfigEnabled(metadata.get("includeRecordOp"))
                && !SourceMetadataSupport.hasConfiguredSourceColumn(config, "recordOpSourceColumn")) {
            throw new ServiceException("逻辑节点“" + nodeName + "”已启用操作类型，但未选择源字段");
        }
    }

    /**
     * 逻辑节点会自动向 writer 增加治理列，但不会修改生产表结构。
     * 发布前读取目标表字段，提前拦截运行时才发现列不存在的任务。
     */
    @SuppressWarnings("unchecked")
    private void validatePipelineTargetGovernanceColumns(Map<String, Object> config, String nodeName) {
        if (config == null) {
            return;
        }
        Map<String, Object> output = config.get("output") instanceof Map
                ? (Map<String, Object>) config.get("output") : Collections.emptyMap();
        Map<String, Object> datasource = output.get("writerDatasource") instanceof Map
                ? (Map<String, Object>) output.get("writerDatasource") : Collections.emptyMap();
        Long datasourceId = toLong(firstNonNull(datasource.get("datasourceId"),
                datasource.get("id"), datasource.get("datasource_id")));
        String tableName = firstConfigValue(output, "table", "target_table_name");
        if (datasourceId == null || StringUtils.isBlank(tableName)) {
            return;
        }
        List<DbColumn> targetColumns;
        try {
            targetColumns = datasourceApiService.getDbTableColumns(datasourceId, tableName);
        } catch (Exception ex) {
            throw new ServiceException("逻辑节点“" + nodeName + "”无法读取目标表结构，请确认目标数据源、表名和权限");
        }
        if (CollectionUtils.isEmpty(targetColumns)) {
            throw new ServiceException("逻辑节点“" + nodeName + "”目标表不存在或没有字段: " + tableName);
        }
        Set<String> actual = targetColumns.stream()
                .filter(Objects::nonNull)
                .map(DbColumn::getColName)
                .filter(StringUtils::isNotBlank)
                .map(value -> value.toLowerCase(Locale.ROOT))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<String> required = SourceMetadataSupport.metadataColumns(config);
        List<String> missing = required.stream()
                .filter(StringUtils::isNotBlank)
                .filter(value -> !actual.contains(value.toLowerCase(Locale.ROOT)))
                .distinct()
                .collect(Collectors.toList());
        if (!missing.isEmpty()) {
            throw new ServiceException("逻辑节点“" + nodeName + "”目标表缺少治理列: "
                    + String.join(", ", missing));
        }
    }

    private Object firstNonNull(Object first, Object second, Object third) {
        return first != null ? first : (second != null ? second : third);
    }

    private Long toLong(Object value) {
        if (value == null || StringUtils.isBlank(String.valueOf(value))) return null;
        try {
            return Long.valueOf(String.valueOf(value).trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private void validatePipelineDataSources(Map<String, Object> config, String nodeName) {
        if (config == null) return;
        Map<String, Object> output = config.get("output") instanceof Map
                ? (Map<String, Object>) config.get("output") : Collections.emptyMap();
        if (isDatasourceMissing(output.get("writerDatasource"))) {
            throw new ServiceException("逻辑节点“" + nodeName + "”未配置目标数据连接");
        }
        if (StringUtils.isBlank(firstConfigValue(output, "table", "target_table_name"))) {
            throw new ServiceException("逻辑节点“" + nodeName + "”未配置目标表");
        }
        String mode = String.valueOf(config.getOrDefault("mode", "INTEGRATION"));
        if ("MERGE".equalsIgnoreCase(mode) && isDatasourceMissing(config.get("readerDatasource"))) {
            throw new ServiceException("合并节点“" + nodeName + "”未配置读取数据连接");
        }
        if ("INTEGRATION".equalsIgnoreCase(mode)) {
            Object inputs = config.get("inputs");
            boolean hasUpstream = false;
            if (inputs instanceof List && !((List<?>) inputs).isEmpty() && ((List<?>) inputs).get(0) instanceof Map) {
                Map<String, Object> first = (Map<String, Object>) ((List<?>) inputs).get(0);
                hasUpstream = StringUtils.isNotBlank(firstConfigValue(first, "refNodeCode", "refNodeId"));
            }
            if (!hasUpstream) {
                Map<String, Object> input = config.get("input") instanceof Map
                        ? (Map<String, Object>) config.get("input") : Collections.emptyMap();
                Object readerDatasource = input.get("readerDatasource");
                if (isDatasourceMissing(readerDatasource)) readerDatasource = config.get("readerDatasource");
                if (isDatasourceMissing(readerDatasource)) {
                    throw new ServiceException("逻辑节点“" + nodeName + "”未配置源数据连接");
                }
                if (StringUtils.isBlank(firstConfigValue(input, "table", "table_name"))
                        && StringUtils.isBlank(firstConfigValue(config, "sourceTable", "table_name"))) {
                    throw new ServiceException("逻辑节点“" + nodeName + "”未配置源表");
                }
            }
        } else if ("MERGE".equalsIgnoreCase(mode)) {
            Object inputs = config.get("inputs");
            if (!(inputs instanceof List) || ((List<?>) inputs).size() < 2) {
                throw new ServiceException("合并节点“" + nodeName + "”至少需要两个上游输入");
            }
            for (Object item : (List<?>) inputs) {
                if (!(item instanceof Map)
                        || StringUtils.isBlank(firstConfigValue((Map<String, Object>) item, "table", "table_name"))) {
                    throw new ServiceException("合并节点“" + nodeName + "”存在未解析的上游目标表");
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void validatePipelineMergeInputs(Map<String, Object> config, String nodeName,
                                             String configuredCode) {
        Map<String, Object> mergeConfig = config != null && config.get("mergeConfig") instanceof Map
                ? (Map<String, Object>) config.get("mergeConfig") : Collections.emptyMap();
        String mergeType = String.valueOf(mergeConfig.getOrDefault("type", "UNION_ALL"));
        if (!Arrays.asList("UNION_ALL", "JOIN").contains(mergeType.toUpperCase(Locale.ROOT))) {
            throw new ServiceException("合并节点“" + nodeName + "”使用了不支持的合并方式");
        }
        Object inputs = config == null ? null : config.get("inputs");
        if (!(inputs instanceof List) || ((List<?>) inputs).size() < 2) {
            throw new ServiceException("合并节点“" + nodeName + "”至少需要两个上游输入");
        }
        Set<String> refs = new HashSet<>();
        for (Object item : (List<?>) inputs) {
            if (!(item instanceof Map)) {
                throw new ServiceException("合并节点“" + nodeName + "”的输入必须引用上游逻辑节点");
            }
            Map<String, Object> input = (Map<String, Object>) item;
            String ref = firstConfigValue(input, "refNodeCode", "refNodeId");
            if (StringUtils.isBlank(ref)) {
                throw new ServiceException("合并节点“" + nodeName + "”的每个输入都必须选择上游逻辑节点");
            }
            if (StringUtils.equals(ref, configuredCode)
                    || StringUtils.equals(ref, String.valueOf(config.get("pipelineNodeId")))
                    || StringUtils.equals(ref, String.valueOf(config.get("nodeCode")))) {
                throw new ServiceException("合并节点“" + nodeName + "”不能引用自身");
            }
            if (!refs.add(ref)) {
                throw new ServiceException("合并节点“" + nodeName + "”不能重复引用同一个上游节点");
            }
        }
    }

    @SuppressWarnings("unchecked")
    private String resolvePipelineNodeKey(CollectorEtlNodeSaveReqVO node) {
        if (node == null) return null;
        String nestedId = null;
        Map<String, Object> params = node.getTaskParams();
        if (params != null && params.get("pipelineNode") instanceof Map) {
            Object value = ((Map<String, Object>) params.get("pipelineNode")).get("pipelineNodeId");
            if (value != null) nestedId = String.valueOf(value);
        }
        return StringUtils.defaultIfBlank(node.getPipelineNodeId(),
                StringUtils.defaultIfBlank(nestedId,
                        StringUtils.defaultIfBlank(node.getCode(), node.getName())));
    }

    @SuppressWarnings("unchecked")
    private void preparePipelineInputs(CollectorEtlNodeSaveReqVO node, String configuredCode,
                                    Map<String, Map<String, Object>> pipelineConfigByCode) {
        if (node == null || node.getTaskParams() == null) return;
        Map<String, Object> pipeline = pipelineConfigByCode.get(configuredCode);
        if (pipeline == null) return;
        Object inputs = pipeline.get("inputs");
        String mode = String.valueOf(pipeline.get("mode"));
        if ("INTEGRATION".equalsIgnoreCase(mode)) {
            if (!(inputs instanceof List) || ((List<?>) inputs).isEmpty()) return;
            Object first = ((List<?>) inputs).get(0);
            if (!(first instanceof Map)) return;
            Map<String, Object> inputRef = (Map<String, Object>) first;
            String ref = firstConfigValue(inputRef, "refNodeCode", "refNodeId");
            if (StringUtils.isBlank(ref)) return;
            ref = resolvePipelineConfigKey(ref, pipelineConfigByCode);
            Map<String, Object> upstream = pipelineConfigByCode.get(ref);
            if (upstream == null) {
                throw new ServiceException("节点“" + node.getName() + "”引用了不存在的上游节点: " + ref);
            }
            if (!(upstream.get("output") instanceof Map)) {
                throw new ServiceException("上游节点“" + ref + "”缺少输出配置");
            }
            Map<String, Object> upstreamOutput = (Map<String, Object>) upstream.get("output");
            String table = firstConfigValue(upstreamOutput, "table", "target_table_name");
            if (StringUtils.isBlank(table)) {
                throw new ServiceException("上游节点“" + ref + "”未配置目标表");
            }
            Map<String, Object> input = pipeline.get("input") instanceof Map
                    ? new LinkedHashMap<>((Map<String, Object>) pipeline.get("input")) : new LinkedHashMap<>();
            input.put("table", table);
            input.put("table_name", table);
            if (upstreamOutput.get("writerDatasource") instanceof Map) {
                // NODE_OUTPUT 的物理表属于上游 writer 数据源，不能继续使用节点原来残留的源连接。
                Map<String, Object> upstreamDatasource = new LinkedHashMap<>(
                        (Map<String, Object>) upstreamOutput.get("writerDatasource"));
                input.put("readerDatasource", upstreamDatasource);
                pipeline.put("readerDatasource", upstreamDatasource);
            }
            pipeline.put("input", input);
            pipeline.put("sourceTable", table);
            preservePipelineSourceMetadata(pipeline, Collections.singletonList(upstream), pipelineConfigByCode);
            node.getTaskParams().put("pipelineNode", pipeline);
            return;
        }
        if (!(inputs instanceof List)) return;
        List<Map<String, Object>> resolved = new ArrayList<>();
        for (Object item : (List<?>) inputs) {
            if (!(item instanceof Map)) continue;
            Map<String, Object> source = new LinkedHashMap<>((Map<String, Object>) item);
            String ref = firstConfigValue(source, "refNodeCode", "refNodeId");
            if ((source.get("table") == null || StringUtils.isBlank(String.valueOf(source.get("table"))))
                    && StringUtils.isNotBlank(ref)) {
                ref = resolvePipelineConfigKey(ref, pipelineConfigByCode);
                Map<String, Object> upstream = pipelineConfigByCode.get(ref);
                if (upstream == null) {
                    throw new ServiceException("合并节点“" + node.getName() + "”引用了不存在的上游节点: " + ref);
                }
                if (upstream != null && upstream.get("output") instanceof Map) {
                    Map<String, Object> output = (Map<String, Object>) upstream.get("output");
                    String table = firstConfigValue(output, "table", "target_table_name");
                    if (StringUtils.isBlank(table)) {
                        throw new ServiceException("上游节点“" + ref + "”未配置目标表，无法生成合并 SQL");
                    }
                    source.put("table", table);
                } else if (upstream != null) {
                    throw new ServiceException("上游节点“" + ref + "”缺少输出配置，无法生成合并 SQL");
                }
            }
            resolved.add(source);
        }
        pipeline.put("inputs", resolved);
        List<Map<String, Object>> upstreamConfigs = new ArrayList<>();
        for (Map<String, Object> item : resolved) {
            String ref = firstConfigValue(item, "refNodeCode", "refNodeId");
            if (StringUtils.isBlank(ref)) continue;
            ref = resolvePipelineConfigKey(ref, pipelineConfigByCode);
            Map<String, Object> upstream = pipelineConfigByCode.get(ref);
            if (upstream != null) upstreamConfigs.add(upstream);
        }
        if (pipeline.get("joinConfig") instanceof Map) {
            validatePipelineMergeDatasource(node.getName(), pipeline, upstreamConfigs);
            for (Map<String, Object> source : resolved) {
                String ref = resolvePipelineConfigKey(firstConfigValue(source, "refNodeCode", "refNodeId"), pipelineConfigByCode);
                Map<String, Object> upstream = pipelineConfigByCode.get(ref);
                Map<String, Object> output = upstream == null ? Collections.emptyMap() : (Map<String, Object>) upstream.get("output");
                source.put("columns", output.get("target_columns"));
                source.put("refNodeId", ref);
                source.put("table_name", source.get("table"));
            }
            // JOIN 结果是新的复合来源，不继承任意一侧单表追溯列。
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("enabled", true); metadata.put("includeNaturalKey", false);
            metadata.put("sourceTableColumn", "source_table"); metadata.put("writeTimeColumn", "write_time");
            pipeline.put("sourceMetadata", metadata);
            node.getTaskParams().put("pipelineNode", pipeline);
            return;
        }
        if (!upstreamConfigs.isEmpty()) {
            validatePipelineMergeDatasource(node.getName(), pipeline, upstreamConfigs);
            validatePipelineMergeMetadataColumns(node.getName(), upstreamConfigs, pipelineConfigByCode);
            validatePipelineMergeBusinessColumns(node.getName(), pipeline, upstreamConfigs);
            preservePipelineSourceMetadata(pipeline, upstreamConfigs, pipelineConfigByCode);
        }
        node.getTaskParams().put("pipelineNode", pipeline);
    }

    @SuppressWarnings("unchecked")
    private void preservePipelineSourceMetadata(Map<String, Object> pipeline,
                                                Collection<Map<String, Object>> upstreamConfigs,
                                                Map<String, Map<String, Object>> pipelineConfigByCode) {
        if (pipeline == null) return;
        Map<String, Object> metadata = pipeline.get("sourceMetadata") instanceof Map
                ? new LinkedHashMap<>((Map<String, Object>) pipeline.get("sourceMetadata"))
                : new LinkedHashMap<>();
        metadata.put("enabled", true);
        metadata.put("preserveExistingSource", true);
        List<String> preservedColumns = new ArrayList<>();
        if (upstreamConfigs != null) {
            for (Map<String, Object> upstream : upstreamConfigs) {
                for (String column : collectPipelineMetadataColumns(upstream, pipelineConfigByCode,
                        new HashSet<String>())) {
                    if (!preservedColumns.contains(column)) preservedColumns.add(column);
                }
            }
        }
        metadata.put("preservedMetadataColumns", preservedColumns);
        pipeline.put("sourceMetadata", metadata);

        // 下游 reader 必须把上游已有的来源列读出来，转换层才能继续传递自然键/来源表。
        List<Object> columns = new ArrayList<>();
        if (pipeline.get("columns") instanceof List) {
            columns.addAll((List<Object>) pipeline.get("columns"));
        }
        for (String column : SourceMetadataSupport.metadataColumns(pipeline)) {
            boolean exists = false;
            for (Object item : columns) {
                String itemName = item instanceof Map
                        ? String.valueOf(((Map<?, ?>) item).get("columnName"))
                        : String.valueOf(item);
                if (item instanceof Map && ("null".equals(itemName) || itemName.trim().isEmpty())) {
                    itemName = String.valueOf(((Map<?, ?>) item).get("name"));
                }
                if (column.equalsIgnoreCase(itemName)) {
                    exists = true;
                    break;
                }
            }
            if (!exists) columns.add(column);
        }
        pipeline.put("columns", columns);
        if (pipeline.get("input") instanceof Map) {
            Map<String, Object> input = new LinkedHashMap<>((Map<String, Object>) pipeline.get("input"));
            input.put("columns", columns);
            pipeline.put("input", input);
        }
    }

    /**
     * 递归收集上游链路已经落库的治理列。这样 A -> B -> C 的连续输出场景中，
     * C 即使在发布遍历顺序早于 B，也不会丢失 A 的自然键、行 Hash 等字段。
     */
    @SuppressWarnings("unchecked")
    private List<String> collectPipelineMetadataColumns(Map<String, Object> pipeline,
                                                        Map<String, Map<String, Object>> pipelineConfigByCode,
                                                        Set<String> visited) {
        List<String> result = new ArrayList<>();
        if (pipeline == null) return result;
        for (String column : SourceMetadataSupport.metadataColumns(pipeline)) {
            if (!result.contains(column)) result.add(column);
        }
        // JOIN 是复合来源的物化边界，不能递归带回任意单表的自然键/行 Hash。
        if (pipeline.get("joinConfig") instanceof Map) return result;
        Object inputs = pipeline.get("inputs");
        if (!(inputs instanceof List) || pipelineConfigByCode == null) return result;
        for (Object item : (List<?>) inputs) {
            if (!(item instanceof Map)) continue;
            Map<String, Object> input = (Map<String, Object>) item;
            String ref = firstConfigValue(input, "refNodeCode", "refNodeId");
            if (StringUtils.isBlank(ref)) continue;
            ref = resolvePipelineConfigKey(ref, pipelineConfigByCode);
            if (!visited.add(ref)) continue;
            Map<String, Object> upstream = pipelineConfigByCode.get(ref);
            for (String column : collectPipelineMetadataColumns(upstream, pipelineConfigByCode, visited)) {
                if (!result.contains(column)) result.add(column);
            }
        }
        return result;
    }

    private void validatePipelineMergeMetadataColumns(String nodeName,
                                                       Collection<Map<String, Object>> upstreamConfigs,
                                                       Map<String, Map<String, Object>> pipelineConfigByCode) {
        Set<String> expected = null;
        for (Map<String, Object> upstream : upstreamConfigs) {
            Set<String> current = collectPipelineMetadataColumns(upstream, pipelineConfigByCode,
                    new HashSet<String>()).stream()
                    .map(value -> value.toLowerCase(Locale.ROOT))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            if (expected == null) {
                expected = current;
            } else if (!expected.equals(current)) {
                throw new ServiceException("合并节点“" + nodeName
                        + "”的上游来源治理列不一致，请统一 sourceMetadata 配置后再合并");
            }
        }
    }

    /** UNION ALL 必须在所有分支上读取同一组业务列；治理列单独由上面的规则校验。 */
    @SuppressWarnings("unchecked")
    private void validatePipelineMergeBusinessColumns(String nodeName,
                                                      Map<String, Object> mergePipeline,
                                                      Collection<Map<String, Object>> upstreamConfigs) {
        Set<String> governanceColumns = new HashSet<>();
        governanceColumns.addAll(SourceMetadataSupport.metadataColumns(mergePipeline).stream()
                .map(value -> value.toLowerCase(Locale.ROOT)).collect(Collectors.toSet()));
        for (Map<String, Object> upstream : upstreamConfigs) {
            governanceColumns.addAll(collectPipelineMetadataColumns(upstream, null,
                    new HashSet<String>()).stream()
                    .map(value -> value.toLowerCase(Locale.ROOT)).collect(Collectors.toSet()));
        }
        Set<String> expected = null;
        for (Map<String, Object> upstream : upstreamConfigs) {
            Set<String> current = configuredPipelineBusinessColumns(upstream, governanceColumns);
            if (current.isEmpty()) {
                throw new ServiceException("合并节点“" + nodeName + "”存在未配置业务字段的上游节点");
            }
            if (expected == null) {
                expected = current;
            } else if (!expected.equals(current)) {
                throw new ServiceException("合并节点“" + nodeName
                        + "”的上游业务字段不一致，UNION ALL 只能合并结构相同的表");
            }
        }
        Set<String> configured = configuredPipelineBusinessColumns(mergePipeline, governanceColumns);
        if (!configured.isEmpty() && expected != null && !expected.equals(configured)) {
            throw new ServiceException("合并节点“" + nodeName
                    + "”当前字段配置与上游表结构不一致，请重新读取上游字段");
        }
    }

    @SuppressWarnings("unchecked")
    private Set<String> configuredPipelineBusinessColumns(Map<String, Object> pipeline,
                                                           Set<String> governanceColumns) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        if (pipeline == null) return result;
        Map<String, Object> output = pipeline.get("output") instanceof Map
                ? (Map<String, Object>) pipeline.get("output") : Collections.emptyMap();
        Object raw = output.get("target_columns");
        if (isEmptyPipelineColumns(raw)) raw = output.get("columns");
        if (isEmptyPipelineColumns(raw)) raw = pipeline.get("targetColumns");
        if (isEmptyPipelineColumns(raw)) raw = pipeline.get("columns");
        List<String> columns = normalizePipelineColumnNames(raw);
        for (String column : columns) {
            if (governanceColumns == null
                    || !governanceColumns.contains(column.toLowerCase(Locale.ROOT))) {
                result.add(column.toLowerCase(Locale.ROOT));
            }
        }
        return result;
    }

    private boolean isEmptyPipelineColumns(Object value) {
        if (value == null) return true;
        if (value instanceof Collection) return ((Collection<?>) value).isEmpty();
        return StringUtils.isBlank(String.valueOf(value));
    }

    private List<String> normalizePipelineColumnNames(Object value) {
        List<String> result = new ArrayList<>();
        if (value instanceof Collection) {
            for (Object item : (Collection<?>) value) {
                addPipelineColumnName(result, item);
            }
            return result;
        }
        if (value != null) {
            for (String item : String.valueOf(value).split(",")) {
                addPipelineColumnName(result, item);
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private void addPipelineColumnName(List<String> target, Object item) {
        if (item == null) return;
        String name;
        if (item instanceof Map) {
            Map<String, Object> field = (Map<String, Object>) item;
            name = configText(field.get("columnName"));
            if (StringUtils.isBlank(name)) name = configText(field.get("name"));
        } else {
            name = String.valueOf(item).trim();
        }
        if (StringUtils.isNotBlank(name) && !target.contains(name)) target.add(name);
    }

    @SuppressWarnings("unchecked")
    private void validatePipelineMergeDatasource(String nodeName,
                                                 Map<String, Object> pipeline,
                                                 Collection<Map<String, Object>> upstreamConfigs) {
        Map<String, Object> readerDatasource = pipeline.get("readerDatasource") instanceof Map
                ? (Map<String, Object>) pipeline.get("readerDatasource") : Collections.emptyMap();
        String readerId = datasourceIdentity(readerDatasource);
        if (StringUtils.isBlank(readerId)) return;
        for (Map<String, Object> upstream : upstreamConfigs) {
            Map<String, Object> output = upstream != null && upstream.get("output") instanceof Map
                    ? (Map<String, Object>) upstream.get("output") : Collections.emptyMap();
            Map<String, Object> writerDatasource = output.get("writerDatasource") instanceof Map
                    ? (Map<String, Object>) output.get("writerDatasource") : Collections.emptyMap();
            String writerId = datasourceIdentity(writerDatasource);
            if (StringUtils.isNotBlank(writerId) && !StringUtils.equals(readerId, writerId)) {
                throw new ServiceException("合并节点“" + nodeName
                        + "”的读取数据源必须与所有上游节点的目标数据源一致");
            }
        }
    }

    private String datasourceIdentity(Map<String, Object> datasource) {
        if (datasource == null) return null;
        Object value = datasource.get("datasourceId");
        if (value == null) value = datasource.get("id");
        if (value == null) value = datasource.get("datasource_id");
        return value == null ? null : String.valueOf(value).trim();
    }

    @SuppressWarnings("unchecked")
    private void addPipelineAuxiliaryNodes(Map<String, Object> config, String nodeName, String configuredCode,
                                           CollectorEtlNewNodeSaveReqVO reqVO, List<Map<String, Object>> definitions,
                                           List<Map<String, Object>> locations, Map<String, String> beforeNode,
                                           Map<String, String> afterNode, int index) {
        Map<String, Object> schema = config.get("schemaMonitoring") instanceof Map
                ? (Map<String, Object>) config.get("schemaMonitoring") : Collections.emptyMap();
        if (schema.isEmpty() && config.get("input") instanceof Map
                && ((Map<?, ?>) config.get("input")).get("schemaMonitoring") instanceof Map) {
            schema = (Map<String, Object>) ((Map<?, ?>) config.get("input")).get("schemaMonitoring");
        }
        String schemaTaskId = firstConfigValue(schema, "taskId", "catalogTaskId");
        if (isConfigEnabled(schema.get("enabled")) && StringUtils.isNotBlank(schemaTaskId)) {
            String code = nextDsNodeCode(reqVO.getSpaceCode());
            definitions.add(TaskConverter.buildHttpTaskDefinitionMap(null, nodeName + "-元数据探查", code, 0,
                    appendTaskId(catalogTaskExecuteUrl, schemaTaskId), reqVO.getDraftJson(), getProjectWorkerGroup(reqVO.getSpaceCode())));
            beforeNode.put(configuredCode, code);
            addLocation(locations, code, 80 + index * 260, 120 + (index % 4) * 160);
        }
        Map<String, Object> output = config.get("output") instanceof Map
                ? (Map<String, Object>) config.get("output") : Collections.emptyMap();
        Map<String, Object> quality = output.get("qualityConfig") instanceof Map
                ? (Map<String, Object>) output.get("qualityConfig") : Collections.emptyMap();
        String qualityTaskId = firstConfigValue(quality, "taskId", "qualityTaskId");
        if (isConfigEnabled(quality.get("enabled")) && StringUtils.isNotBlank(qualityTaskId)) {
            String code = nextDsNodeCode(reqVO.getSpaceCode());
            definitions.add(TaskConverter.buildHttpTaskDefinitionMap(null, nodeName + "-质量检查", code, 0,
                    appendTaskId(qualityTaskExecuteUrl, qualityTaskId), reqVO.getDraftJson(), getProjectWorkerGroup(reqVO.getSpaceCode())));
            afterNode.put(configuredCode, code);
            addLocation(locations, code, 360 + index * 260, 120 + (index % 4) * 160);
        }
    }

    private String firstConfigValue(Map<String, Object> map, String first, String second) {
        String value = map == null || map.get(first) == null ? null : String.valueOf(map.get(first)).trim();
        if (StringUtils.isBlank(value) || "null".equalsIgnoreCase(value)) {
            value = map == null || map.get(second) == null ? null : String.valueOf(map.get(second)).trim();
        }
        return StringUtils.isBlank(value) || "null".equalsIgnoreCase(value) ? null : value;
    }

    private String configText(Object value) {
        if (value == null) return null;
        String text = String.valueOf(value).trim();
        return StringUtils.isBlank(text) || "null".equalsIgnoreCase(text) ? null : text;
    }

    private boolean isConfigEnabled(Object value) {
        return Boolean.TRUE.equals(value) || "true".equalsIgnoreCase(String.valueOf(value)) || "1".equals(String.valueOf(value));
    }

    @SuppressWarnings("unchecked")
    private boolean isDatasourceMissing(Object datasource) {
        if (!(datasource instanceof Map)) return true;
        Object id = ((Map<String, Object>) datasource).get("datasourceId");
        if (id == null) id = ((Map<String, Object>) datasource).get("id");
        if (id == null) id = ((Map<String, Object>) datasource).get("datasource_id");
        return id == null || StringUtils.isBlank(String.valueOf(id));
    }

    private String appendTaskId(String baseUrl, String taskId) {
        String base = StringUtils.isBlank(baseUrl) ? "" : baseUrl.trim();
        return base.replaceAll("/$", "") + "/" + taskId;
    }

    private void addLocation(List<Map<String, Object>> locations, String code, double x, double y) {
        Map<String, Object> location = new HashMap<>();
        location.put("taskCode", Long.parseLong(code));
        location.put("x", x);
        location.put("y", y);
        locations.add(location);
    }

    private String resolveConfiguredCode(String value, Map<String, String> nodeIdToConfiguredCode) {
        return nodeIdToConfiguredCode.getOrDefault(value, value);
    }

    /** 将旧草稿中的 nodeCode / pipelineNodeId 引用归一化为当前配置索引键。 */
    @SuppressWarnings("unchecked")
    private String resolvePipelineConfigKey(String value,
                                            Map<String, Map<String, Object>> pipelineConfigByCode) {
        if (StringUtils.isBlank(value) || pipelineConfigByCode == null) return value;
        if (pipelineConfigByCode.containsKey(value)) return value;
        for (Map.Entry<String, Map<String, Object>> entry : pipelineConfigByCode.entrySet()) {
            Map<String, Object> pipeline = entry.getValue();
            if (pipeline == null) continue;
            Object id = pipeline.get("pipelineNodeId");
            Object code = pipeline.get("nodeCode");
            if (value.equals(String.valueOf(id)) || value.equals(String.valueOf(code))) {
                return entry.getKey();
            }
        }
        return value;
    }

    private void addPipelineRelation(List<Map<String, Object>> relations, Set<String> relationKeys,
                                     String pre, String post) {
        if (StringUtils.isBlank(post) || StringUtils.equals(pre, post)) return;
        String key = pre + "->" + post;
        if (!relationKeys.add(key)) return;
        Map<String, Object> relation = new LinkedHashMap<>();
        relation.put("id", null);
        relation.put("preTaskCode", Long.parseLong(pre));
        relation.put("preTaskVersion", 0);
        relation.put("postTaskCode", Long.parseLong(post));
        relation.put("postTaskVersion", 0);
        relation.put("conditionType", "NONE");
        relations.add(relation);
    }

    private void publishPipelineScheduler(CollectorEtlTaskDO taskDO, CollectorEtlNewNodeSaveReqVO reqVO,
                                          String taskCode) {
        CollectorEtlSchedulerDO schedulerDO = getCollectorEtlScheduler(taskCode, taskDO.getId());
        if (schedulerDO == null || schedulerDO.getId() == null) {
            schedulerDO = getCollectorEtlScheduler(null, taskDO.getId());
        }
        if ((schedulerDO == null || schedulerDO.getId() == null) && StringUtils.isNotBlank(reqVO.getCrontab())) {
            schedulerDO = iCollectorEtlSchedulerService.createCollectorEtlSchedulerNew(
                    TaskConverter.convertToCollectorEtlSchedulerSaveReqVO(taskDO.getId(), taskCode, reqVO));
        }
        if (schedulerDO != null && schedulerDO.getId() != null && StringUtils.isBlank(schedulerDO.getCronExpression())
                && StringUtils.isNotBlank(reqVO.getCrontab())) {
            schedulerDO.setCronExpression(reqVO.getCrontab());
        }
        if (schedulerDO != null && StringUtils.isNotEmpty(schedulerDO.getCronExpression())) {
            try {
                DsSchedulerRespDTO response = schedulerDO.getDsId() == null || schedulerDO.getDsId() < 1
                        ? createOrUpdateScheduler(schedulerDO, taskDO) : updateExistingScheduler(schedulerDO, taskDO);
                if (response != null && response.getData() != null) {
                    CollectorEtlSchedulerSaveReqVO save = TaskConverter.convertToCollectorEtlSchedulerSaveReqVO(response, taskDO);
                    save.setId(schedulerDO.getId());
                    save.setTaskCode(taskCode);
                    DsStatusRespDTO online = iDsEtlSchedulerService.onlineScheduler(taskDO.getSpaceCode(), save.getDsId());
                    if (isDsStatusSuccess(online)) {
                        save.setStatus("1");
                        iCollectorEtlSchedulerService.updateCollectorEtlScheduler(save);
                    }
                }
            } catch (Exception e) {
                log.warn("逻辑集成任务已发布，但调度器上线失败，taskId={}，原因={}", taskDO.getId(), e.getMessage(), e);
            }
        }
    }

    private static class PipelinePublishPayload {
        private final List<Map<String, Object>> taskDefinitions;
        private final List<Map<String, Object>> taskRelations;
        private final List<Map<String, Object>> locations;
        private final String primaryCode;
        private final String primaryName;
        private final Map<String, Object> lineageArgs;
        private Map<String, Object> runtimeStages = new LinkedHashMap<>();
        private String resourcesJson;
        private String guardNodeCode;

        private PipelinePublishPayload(List<Map<String, Object>> taskDefinitions,
                                       List<Map<String, Object>> taskRelations,
                                       List<Map<String, Object>> locations,
                                       String primaryCode, String primaryName,
                                       Map<String, Object> lineageArgs) {
            this.taskDefinitions = taskDefinitions;
            this.taskRelations = taskRelations;
            this.locations = locations;
            this.primaryCode = primaryCode;
            this.primaryName = primaryName;
            this.lineageArgs = lineageArgs;
        }
    }

    /** Capture published semantics into the local durable queue, never directly write the legacy graph. */
    private void writeTaskLineageSilently(CollectorEtlTaskDO taskDO, Map<String, Object> mainArgs) {
        if (taskDO == null || mainArgs == null || !collectorLineageSyncService.isEnabled()) return;
        collectorLineageSyncService.enqueue(PublishedLineageAssembler.snapshot(taskDO, mainArgs));
    }

    private boolean isStreamingFlinkxTask(Long taskId) {
        if (taskId == null) {
            return false;
        }
        return isStreamingFlinkxTask(CollectorEtlTaskExtService.getByTaskId(taskId));
    }

    private boolean isStreamingFlinkxTask(CollectorEtlTaskExtDO taskExt) {
        return taskExt != null && TaskConverter.isStreamingFlinkxJob(taskExt.getFlinkxJobJson());
    }

    private void disableSchedulerForStreamingTask(CollectorEtlTaskDO taskDO, String taskCode) {
        CollectorEtlSchedulerDO schedulerDO = getCollectorEtlScheduler(taskCode, taskDO.getId());
        if (schedulerDO == null || schedulerDO.getId() == null) {
            schedulerDO = getCollectorEtlScheduler(null, taskDO.getId());
        }
        if (schedulerDO == null || schedulerDO.getId() == null) {
            return;
        }

        if (schedulerDO.getDsId() != null && schedulerDO.getDsId() > 0) {
            try {
                DsStatusRespDTO offlineScheduler = iDsEtlSchedulerService.offlineScheduler(taskDO.getSpaceCode(), schedulerDO.getDsId());
                if (offlineScheduler == null || !Boolean.TRUE.equals(offlineScheduler.getSuccess())) {
                    log.warn("CDC/流式任务跳过调度器上线，但下线已有调度器失败，taskId={}，taskCode={}，schedulerId={}，原因：{}",
                            taskDO.getId(), taskCode, schedulerDO.getDsId(), offlineScheduler == null ? null : offlineScheduler.getMsg());
                }
            } catch (Exception e) {
                log.warn("CDC/流式任务跳过调度器上线，但下线已有调度器异常，taskId={}，taskCode={}，schedulerId={}，原因：{}",
                        taskDO.getId(), taskCode, schedulerDO.getDsId(), e.getMessage(), e);
            }
        }

        CollectorEtlSchedulerSaveReqVO schedulerSaveReqVO = new CollectorEtlSchedulerSaveReqVO();
        schedulerSaveReqVO.setId(schedulerDO.getId());
        schedulerSaveReqVO.setTaskCode(taskCode);
        schedulerSaveReqVO.setStatus("0");
        iCollectorEtlSchedulerService.updateCollectorEtlScheduler(schedulerSaveReqVO);
        log.info("CDC/流式任务不启用定时调度，taskId={}，taskCode={}，schedulerId={}",
                taskDO.getId(), taskCode, schedulerDO.getDsId());
    }

    @Override
    public void unpublishTask(CollectorEtlNewNodeSaveReqVO reqVO) {
        Long taskId = resolveTaskId(reqVO);
        CollectorEtlTaskDO taskDO = CollectorEtlTaskMapper.selectById(taskId);
        if (taskDO == null) {
            throw new ServiceException("任务不存在");
        }
        checkTaskSpaceAccess(taskDO);

        CollectorEtlTaskExtDO taskExt = CollectorEtlTaskExtService.getByTaskId(taskDO.getId());
        String dsCode;
        if (taskExt != null && StringUtils.isNotEmpty(taskExt.getEtlTaskCode())) {
            dsCode = taskExt.getEtlTaskCode();
        } else {
            dsCode = taskDO.getCode();
        }

        boolean component = isComponentSnapshot(taskExt);
        if (component) {
            runGuardMapper.lockTask(taskId);
            componentRecovery.cancelPending(taskId, "用户下线任务，取消自动恢复");
        }
        stopRunningProcessInstances(taskDO.getId(), String.valueOf(taskDO.getSpaceCode()), dsCode);
        if (component && runGuardEnabled) {
            for (com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlRunDO run : runGuardMapper.findActive(taskId)) {
                dsEtlTaskService.stopWorkflowInstance(taskDO.getSpaceCode(), run.getWorkflowInstanceId());
            }
        }
        if (!component) collectorEtlIncrementalService.forceReleaseIncrementalTask(taskDO.getId());

        //下线DS任务
        DsStatusRespDTO dsStatusRespDTO = dsEtlTaskService.releaseTask("OFFLINE", String.valueOf(taskDO.getSpaceCode()), dsCode);
        boolean processDefinitionMissing = isProcessDefinitionMissing(dsStatusRespDTO);
        if (!processDefinitionMissing && !isDsStatusSuccess(dsStatusRespDTO)) {
            throw new ServiceException("卸载任务失败！");
        }

        //下线调度器
        CollectorEtlSchedulerDO schedulerDO = getCollectorEtlScheduler(taskDO.getCode(), taskDO.getId());
        try {
            if (schedulerDO != null && schedulerDO.getDsId() != null && schedulerDO.getDsId() > 0) {
                DsStatusRespDTO offlineScheduler = iDsEtlSchedulerService.offlineScheduler(taskDO.getSpaceCode(), schedulerDO.getDsId());
                if (!isDsStatusSuccess(offlineScheduler)) {
                    log.warn("下线调度器失败(不影响任务卸载)");
                }
            }
        } catch (Exception e) {
            log.warn("调度器下线处理失败(不影响任务卸载): {}", e.getMessage());
        }

        if (schedulerDO != null && schedulerDO.getId() != null) {
            CollectorEtlSchedulerSaveReqVO schedulerSaveReqVO = new CollectorEtlSchedulerSaveReqVO();
            schedulerSaveReqVO.setId(schedulerDO.getId());
            schedulerSaveReqVO.setStatus("0");
            if (processDefinitionMissing) {
                schedulerSaveReqVO.setTaskCode(taskDO.getCode());
                schedulerSaveReqVO.setDsId(-1L);
            }
            iCollectorEtlSchedulerService.updateCollectorEtlScheduler(schedulerSaveReqVO);
        }
        if (processDefinitionMissing && taskExt != null && !component) {
            CollectorEtlTaskExtService.removeById(taskExt.getId());
        }

        //更新任务状态为已下线
        updateTaskStatus(taskDO.getId(), "0");
        collectorLineageSyncService.lifecycle(taskDO, "INACTIVE");
    }

    private void stopRunningProcessInstances(Long taskId, String spaceCode, String dsCode) {
        Set<Long> processInstanceIds = new LinkedHashSet<>();
        Long runningTaskInstanceId = CollectorEtlTaskInstanceService.getRunTaskInstance(taskId);
        if (runningTaskInstanceId != null) {
            CollectorEtlTaskInstanceDO local = CollectorEtlTaskInstanceService.getById(runningTaskInstanceId);
            processInstanceIds.add(local != null && local.getDsId() != null ? local.getDsId() : runningTaskInstanceId);
        }

        try {
            List<com.datamaster.api.ds.api.etl.ds.ProcessInstance> dsInstances =
                    dsEtlExecutorService.listProcessInstances(spaceCode, dsCode);
            if (CollectionUtils.isNotEmpty(dsInstances)) {
                for (com.datamaster.api.ds.api.etl.ds.ProcessInstance instance : dsInstances) {
                    if (instance == null || instance.getId() == null || instance.getState() == null
                            || instance.getState().isFinished()
                            || !StringUtils.equals(dsCode, instance.getProcessDefinitionCode())) {
                        continue;
                    }
                    processInstanceIds.add(instance.getId());
                }
            }
        } catch (Exception e) {
            log.warn("卸载任务时查询DS运行中流程实例异常，taskId={}，spaceCode={}，taskCode={}，原因：{}",
                    taskId, spaceCode, dsCode, e.getMessage(), e);
        }

        for (Long processInstanceId : processInstanceIds) {
            try {
                DsStatusRespDTO stopResult = dsEtlExecutorService.execute(DSExecuteDTO.builder()
                        .processInstanceId(processInstanceId)
                        .executeType(ExecuteType.STOP)
                        .build(), spaceCode);
                if (stopResult == null || !Boolean.TRUE.equals(stopResult.getSuccess())) {
                    log.warn("卸载任务时停止运行中的DS流程实例失败，taskId={}，processInstanceId={}，msg={}",
                            taskId, processInstanceId, stopResult == null ? "无响应" : stopResult.getMsg());
                }
            } catch (Exception e) {
                log.warn("卸载任务时停止运行中的DS流程实例异常，taskId={}，processInstanceId={}",
                        taskId, processInstanceId, e);
            }
        }
    }

    private boolean isProcessDefinitionMissing(DsStatusRespDTO response) {
        return response != null
                && (Integer.valueOf(50003).equals(response.getCode())
                || StringUtils.containsIgnoreCase(response.getMsg(), "does not exist"));
    }

    private boolean isDsStatusSuccess(DsStatusRespDTO response) {
        if (response == null || Boolean.FALSE.equals(response.getSuccess()) || Boolean.FALSE.equals(response.getData())) {
            return false;
        }
        return Boolean.TRUE.equals(response.getSuccess())
                || Boolean.TRUE.equals(response.getData())
                || StringUtils.equalsIgnoreCase(response.getMsg(), "success");
    }

    private DsTaskSaveRespDTO updateDsTaskAllowingOnline(DsTaskSaveReqDTO request, String spaceCode, String taskCode) {
        DsTaskSaveRespDTO response = dsEtlTaskService.updateTask(request, spaceCode, taskCode);
        if (response == null || !StringUtils.containsIgnoreCase(response.getMsg(), "does not allow edit")) {
            return response;
        }
        DsStatusRespDTO offlineResp = dsEtlTaskService.releaseTask("OFFLINE", spaceCode, taskCode);
        if (!isDsStatusSuccess(offlineResp)) {
            throw new ServiceException("更新DS任务前下线失败:" + (offlineResp == null ? "无响应" : offlineResp.getMsg()));
        }
        return dsEtlTaskService.updateTask(request, spaceCode, taskCode);
    }

    private void canonicalizeComponentDatasources(CollectorEtlNewNodeSaveReqVO request, CollectorEtlTaskDO task) {
        List<CollectorEtlNodeSaveReqVO> nodes = JSON.parseArray(request.getTaskDefinitionList(), CollectorEtlNodeSaveReqVO.class);
        Map<Long, Map<String, Object>> registered = new HashMap<>();
        for (CollectorEtlNodeSaveReqVO node : nodes) {
            if (node == null || node.getTaskParams() == null) throw new ServiceException("组件缺少配置");
            Map<String, Object> params = node.getTaskParams();
            for (String key : Arrays.asList("readerDatasource", "writerDatasource")) {
                if (!(params.get(key) instanceof Map)) continue;
                Map<?, ?> supplied = (Map<?, ?>) params.get(key);
                Long id = toLong(supplied.get("datasourceId") == null ? supplied.get("id") : supplied.get("datasourceId"));
                if (id == null || !datasourceApiService.hasDatasourceSpace(id, task.getSpaceId(), task.getSpaceCode())) {
                    throw new ServiceException("组件数据源未授权给当前任务空间");
                }
                Map<String, Object> canonical = registered.get(id);
                if (canonical == null) {
                    com.datamaster.common.datasource.mgmt.api.dto.DatasourceRespDTO source = datasourceApiService.getDatasourceById(id);
                    if (source == null || Boolean.FALSE.equals(source.getValidFlag())) throw new ServiceException("组件数据源不存在或已停用");
                    canonical = JSON.parseObject(JSON.toJSONString(source), Map.class);
                    canonical.put("datasourceId", id);
                    registered.put(id, canonical);
                }
                params.put(key, new LinkedHashMap<>(canonical));
            }
        }
        request.setTaskDefinitionList(JSON.toJSONString(nodes));
    }

    private void bindComponentTaskOwner(CollectorEtlNewNodeSaveReqVO request, CollectorEtlTaskDO task) {
        request.setId(task.getId()); request.setIdStr(String.valueOf(task.getId()));
        request.setSpaceId(task.getSpaceId());
        request.setSpaceCode(JSONUtils.convertToLong(task.getSpaceCode()));
        request.setType(task.getType());
    }

    private Long resolveTaskId(CollectorEtlNewNodeSaveReqVO reqVO) {
        if (reqVO.getId() != null && StringUtils.isNotBlank(reqVO.getIdStr())
                && !reqVO.getId().equals(JSONUtils.convertToLong(reqVO.getIdStr()))) {
            throw new ServiceException("任务ID与idStr不一致");
        }
        if (reqVO.getId() != null) {
            return reqVO.getId();
        }
        if (StringUtils.isNotBlank(reqVO.getIdStr())) {
            return JSONUtils.convertToLong(reqVO.getIdStr());
        }
        throw new ServiceException("任务ID不能为空");
    }

    private CollectorEtlNewNodeSaveReqVO fillPublishPayload(CollectorEtlNewNodeSaveReqVO reqVO) {
        if (StringUtils.isNotBlank(reqVO.getTaskDefinitionList())) {
            Long taskId = resolveTaskId(reqVO);
            reqVO.setId(taskId);
            reqVO.setIdStr(String.valueOf(taskId));
            normalizeProjectCode(reqVO);
            return reqVO;
        }
        Long taskId = resolveTaskId(reqVO);
        CollectorEtlTaskUpdateQueryRespVO saved = this.getuUpdateQueryInfo(taskId);
        if (saved == null || CollectionUtils.isEmpty(saved.getTaskDefinitionList())) {
            throw new ServiceException("任务配置未完成，请先保存任务配置");
        }
        CollectorEtlNewNodeSaveReqVO filled = new CollectorEtlNewNodeSaveReqVO(saved);
        filled.setId(taskId);
        filled.setIdStr(String.valueOf(taskId));
        if (reqVO.getSpaceCode() != null) {
            filled.setSpaceCode(reqVO.getSpaceCode());
        }
        if (reqVO.getSpaceId() != null) {
            filled.setSpaceId(reqVO.getSpaceId());
        }
        normalizeProjectCode(filled);
        return filled;
    }

    private void normalizeProjectCode(CollectorEtlNewNodeSaveReqVO reqVO) {
        if (reqVO.getSpaceId() == null) {
            return;
        }
        String spaceCode = TaxonomySpaceApi.getSpaceCodeBySpaceId(reqVO.getSpaceId());
        if (StringUtils.isNotBlank(spaceCode)) {
            reqVO.setSpaceCode(JSONUtils.convertToLong(spaceCode));
        }
    }


    @Override
    public CollectorEtlTaskUpdateQueryRespVO getupdateQueryFront(Long id) {
        CollectorEtlTaskDO taskForCheck = CollectorEtlTaskMapper.selectById(id);
        checkTaskSpaceAccess(taskForCheck);
        return loadUpdateQueryFront(taskForCheck);
    }

    /** 内部读取已保存定义；新组件运行准入改用发布快照，不再依赖此入口。 */
    @Override
    public CollectorEtlTaskUpdateQueryRespVO getSavedTaskForRunGuard(Long id) {
        return loadUpdateQueryFront(CollectorEtlTaskMapper.selectById(id));
    }

    private CollectorEtlTaskUpdateQueryRespVO loadUpdateQueryFront(CollectorEtlTaskDO CollectorEtlTaskDO) {
        if (CollectorEtlTaskDO == null) throw new ServiceException("任务不存在");
        CollectorEtlTaskUpdateQueryRespVO bean = new CollectorEtlTaskUpdateQueryRespVO(CollectorEtlTaskDO);
        CollectorEtlTaskRespVO task = BeanUtils.toBean(CollectorEtlTaskDO, CollectorEtlTaskRespVO.class);
        List<CollectorEtlTaskNodeRelRespVO> relations = getTaskNodeRelList(task);
        bean.setTaskRelationJsonFromNodeRelList(relations);
        bean.setTaskDefinitionList(removeDuplicateById(getNodeRespListByTaskNodeRelList(relations), task.getType()));

        CollectorEtlSchedulerDO CollectorEtlSchedulerById = getCollectorEtlScheduler(bean.getCode(), bean.getId());
        bean.setCrontab(CollectorEtlSchedulerById.getCronExpression());
        return bean;
    }

    public CollectorEtlSchedulerDO getCollectorEtlScheduler(String taskCode, Long taskId) {
        CollectorEtlSchedulerPageReqVO reqVO = new CollectorEtlSchedulerPageReqVO();
        reqVO.setTaskCode(taskCode);
        reqVO.setTaskId(taskId);
        CollectorEtlSchedulerDO result = iCollectorEtlSchedulerService.getCollectorEtlSchedulerById(reqVO);
        if ((result == null || result.getId() == null) && taskId != null && StringUtils.isNotBlank(taskCode)) {
            reqVO.setTaskCode(null);
            result = iCollectorEtlSchedulerService.getCollectorEtlSchedulerById(reqVO);
        }
        return result == null ? new CollectorEtlSchedulerDO() : result;
    }

    @Override
    public CollectorEtlTaskSaveReqVO copyCreateEtl(CollectorEtlNewNodeSaveReqVO nodeSaveReqVO) {
        CollectorEtlTaskUpdateQueryRespVO CollectorEtlTaskUpdateQueryRespVO = this.getuUpdateQueryInfo(JSONUtils.convertToLong(nodeSaveReqVO.getIdStr()));

        //判断是否是离线任务 是需要获取扩展信息的任务编码进行接口调用
        if (StringUtils.equals("1", CollectorEtlTaskUpdateQueryRespVO.getType())) {
            //获取扩展信息
            CollectorEtlTaskExtDO taskExt = CollectorEtlTaskExtService.getByTaskId(Long.parseLong(nodeSaveReqVO.getIdStr()));
            if (taskExt == null) {
                throw new ServiceException("暂无数据！");
            }
            CollectorEtlTaskUpdateQueryRespVO.setCode(taskExt.getEtlTaskCode());
        }

        DsTaskSaveRespDTO task = dsEtlTaskService.batchCopy(CollectorEtlTaskUpdateQueryRespVO.getCode()
                , CollectorEtlTaskUpdateQueryRespVO.getSpaceCode());

        if (!task.getSuccess()) {
            throw new ServiceException("copy任务错误:" + task.getMsg().toString()); // 抛出任务定义创建错误的异常
        }
        ProcessDefinition data = task.getData();

        //任务类型;1：离线任务 2：实时任务 3：数据开发任务 4：	作业任务
        String type = CollectorEtlTaskUpdateQueryRespVO.getType();
        if (StringUtils.equals("1", type)) {
            return copyCreateEtlTask(CollectorEtlTaskUpdateQueryRespVO, data);
        } else if (StringUtils.equals("2", type)) {
            return copyCreateEtlTaskFrontPostpositionRealTime(CollectorEtlTaskUpdateQueryRespVO, data);
        } else if (StringUtils.equals("3", type)) {
            return copyCreateProcessDefinition(CollectorEtlTaskUpdateQueryRespVO, data);
        } else if (StringUtils.equals("4", type)) {
            return copyCreateProcessDefinition(CollectorEtlTaskUpdateQueryRespVO, data);
        }
        return null;
    }


    private CollectorEtlTaskSaveReqVO copyCreateProcessDefinition(CollectorEtlTaskUpdateQueryRespVO CollectorEtlTaskUpdateQueryRespVO, ProcessDefinition data) {
        return null;
    }

    private CollectorEtlTaskSaveReqVO copyCreateEtlTaskFrontPostpositionRealTime(CollectorEtlTaskUpdateQueryRespVO CollectorEtlTaskUpdateQueryRespVO, ProcessDefinition data) {
        return null;
    }

    /**
     * copy
     *
     * @param src
     * @return
     */
    public CollectorEtlTaskSaveReqVO copyCreateEtlTask(CollectorEtlTaskUpdateQueryRespVO src, ProcessDefinition data) {
        CollectorEtlNewNodeSaveReqVO CollectorEtlNewNodeSaveReqVO = new CollectorEtlNewNodeSaveReqVO(src);
        String taskCode = data.getCode();
        String name = data.getName();
        List<Map<String, Object>> locations = CollectorEtlNewNodeSaveReqVO.getLocations();
        Map<Long, Long> definitionCopyVO = new HashMap<>();

        // 转换任务保存请求对象
        CollectorEtlTaskSaveReqVO taskSaveReqVO = TaskConverter.convertToCollectorEtlTaskSaveReqVO(CollectorEtlNewNodeSaveReqVO, data);
        taskSaveReqVO.setCode(taskCode);
        taskSaveReqVO.setDraftJson(src.getDraftJson());

        for (Map<String, Object> location : locations) {
            Long codeold = MapUtils.getLong(location, "taskCode");
            //生成节点编码
            DsNodeGenCodeRespDTO dsNodeGenCodeRespDTO = dsEtlNodeService.genCode(CollectorEtlNewNodeSaveReqVO.getSpaceCode());
            String codeNew = String.valueOf(dsNodeGenCodeRespDTO.getData().get(0));

            definitionCopyVO.put(codeold, JSONUtils.convertToLong(codeNew));
            location.put("taskCode", codeNew);
        }

        //封装节点编码
        remapTaskCodes(CollectorEtlNewNodeSaveReqVO, definitionCopyVO);

        taskSaveReqVO.setLocations(JSONUtils.toJson(locations));
        Long CollectorEtlTask = this.createCollectorEtlTask(taskSaveReqVO);
        taskSaveReqVO.setId(CollectorEtlTask);
        CollectorEtlTaskExtDO origExt = CollectorEtlTaskExtService.getByTaskId(src.getId());
        boolean copyIncremental = origExt != null && StringUtils.isNotBlank(origExt.getIncrementalType());
        if (copyIncremental) {
            data = rebuildCopiedIncrementalTask(taskSaveReqVO, data);
        }

        //构建任务任务信息
        Map<String, Object> taskInfo = new HashMap<>();
        taskInfo.put("spaceCode", CollectorEtlNewNodeSaveReqVO.getSpaceCode());
        taskInfo.put("taskCode", taskCode);
        taskInfo.put("taskVersion", 1);
        taskInfo.put("name", name);

        // 调度器对象构建
        CollectorEtlSchedulerSaveReqVO schedulerSaveReqVO = TaskConverter.convertToCollectorEtlSchedulerSaveReqVO(
                CollectorEtlTask, taskSaveReqVO.getCode(), CollectorEtlNewNodeSaveReqVO
        );
        iCollectorEtlSchedulerService.createCollectorEtlScheduler(schedulerSaveReqVO);

        CollectorEtlTaskLogSaveReqVO CollectorEtlTaskLogSaveReqVO = TaskConverter.fromCollectorEtlTaskLogSaveReqVO(CollectorEtlNewNodeSaveReqVO, data);
        CollectorEtlTaskLogSaveReqVO.setLocations(JSONUtils.toJson(locations));
        CollectorEtlTaskLogSaveReqVO.setCode(taskCode);
        Long CollectorEtlTaskLog = iCollectorEtlTaskLogService.createCollectorEtlTaskLog(CollectorEtlTaskLogSaveReqVO);
        CollectorEtlTaskLogSaveReqVO.setId(CollectorEtlTaskLog);

        List<CollectorEtlNodeSaveReqVO> CollectorEtlNodeSaveReqVOList = TaskConverter.convertToCollectorEtlNodeSaveReqVOList(CollectorEtlNewNodeSaveReqVO, 1);

        //获取原始扩展数据中的flinkxJobJson（如果有）
        String copyFlinkxJobJson = origExt == null ? null
                : (copyIncremental ? origExt.getFlinkxJobTemplateJson() : origExt.getFlinkxJobJson());
        TaskDefinition copiedEtlNode = copyIncremental
                ? getDsTaskDefinitionByType(data, "CHUNJUN")
                : data.getTaskDefinitionList().get(0);
        ProcessTaskRelation copiedEtlRelation = getDsTaskRelation(data, copiedEtlNode.getCode());

        //创建etl任务扩展数据
        CollectorEtlTaskExtSaveReqVO extSaveReqVO = CollectorEtlTaskExtSaveReqVO.builder()
                .taskId(CollectorEtlTask)
                .etlTaskCode(data.getCode())
                .etlTaskVersion(data.getVersion())
                .etlNodeId(copiedEtlNode.getId())
                .etlNodeName(copiedEtlNode.getName())
                .etlNodeCode(copiedEtlNode.getCode())
                .etlNodeVersion(copiedEtlNode.getVersion())
                .etlRelationId(copiedEtlRelation.getId())
                .build();
        if (copyFlinkxJobJson != null) {
            extSaveReqVO.setFlinkxJobJson(copyFlinkxJobJson);
        }
        if (copyIncremental) {
            TaskDefinition copiedPrepareNode = getDsPreviousTaskDefinition(data, copiedEtlNode.getCode());
            TaskDefinition copiedCompleteNode = getDsNextTaskDefinition(data, copiedEtlNode.getCode());
            ProcessTaskRelation copiedPrepareRelation = getDsTaskRelation(data, copiedPrepareNode.getCode());
            ProcessTaskRelation copiedCompleteRelation = getDsTaskRelation(data, copiedCompleteNode.getCode());
            extSaveReqVO.setFlinkxJobTemplateJson(origExt.getFlinkxJobTemplateJson());
            extSaveReqVO.setIncrementalType(origExt.getIncrementalType());
            extSaveReqVO.setSourceDatasourceId(origExt.getSourceDatasourceId());
            extSaveReqVO.setTargetDatasourceId(origExt.getTargetDatasourceId());
            extSaveReqVO.setSourceTableName(origExt.getSourceTableName());
            extSaveReqVO.setTargetTableName(origExt.getTargetTableName());
            extSaveReqVO.setSourceIncrementColumn(origExt.getSourceIncrementColumn());
            extSaveReqVO.setTargetIncrementColumn(origExt.getTargetIncrementColumn());
            extSaveReqVO.setIncrementalInitialValue(resolveCopiedIncrementalInitialValue(src, origExt));
            extSaveReqVO.setIncrementalTimeFormat(origExt.getIncrementalTimeFormat());
            extSaveReqVO.setPrepareNodeId(copiedPrepareNode.getId());
            extSaveReqVO.setPrepareNodeName(copiedPrepareNode.getName());
            extSaveReqVO.setPrepareNodeCode(copiedPrepareNode.getCode());
            extSaveReqVO.setPrepareNodeVersion(copiedPrepareNode.getVersion());
            extSaveReqVO.setPrepareRelationId(copiedPrepareRelation.getId());
            extSaveReqVO.setCompleteNodeId(copiedCompleteNode.getId());
            extSaveReqVO.setCompleteNodeName(copiedCompleteNode.getName());
            extSaveReqVO.setCompleteNodeCode(copiedCompleteNode.getCode());
            extSaveReqVO.setCompleteNodeVersion(copiedCompleteNode.getVersion());
            extSaveReqVO.setCompleteRelationId(copiedCompleteRelation.getId());
        }
        CollectorEtlTaskExtService.createCollectorEtlTaskExt(extSaveReqVO);

        List<CollectorEtlNodeDO> CollectorEtlNodeBatch = iCollectorEtlNodeService.createCollectorEtlNodeBatch(CollectorEtlNodeSaveReqVOList);

        List<CollectorEtlNodeLogSaveReqVO> CollectorEtlNodeLogSaveReqVOS = TaskConverter.convertToCollectorEtlNodeLogSaveReqVOList(CollectorEtlNodeSaveReqVOList);
        iCollectorEtlNodeLogService.createCollectorEtlNodeLogBatch(CollectorEtlNodeLogSaveReqVOS);

        List<CollectorEtlTaskNodeRelSaveReqVO> CollectorEtlTaskNodeRelSaveReqVOS = TaskConverter.convertToCollectorEtlTaskNodeRelSaveReqVOList(CollectorEtlNodeBatch, CollectorEtlNewNodeSaveReqVO, taskSaveReqVO);
        iCollectorEtlTaskNodeRelService.createCollectorEtlTaskNodeRelBatch(CollectorEtlTaskNodeRelSaveReqVOS);

        List<CollectorEtlTaskNodeRelLogSaveReqVO> CollectorEtlTaskNodeRelLogSaveReqVOS = TaskConverter.convertToCollectorEtlTaskNodeRelLogSaveReqVOList(CollectorEtlTaskNodeRelSaveReqVOS);
        iCollectorEtlTaskNodeRelLogService.createCollectorEtlTaskNodeRelLogBatch(CollectorEtlTaskNodeRelLogSaveReqVOS);

        return taskSaveReqVO; // 返回创建结果
    }


    public static void remapTaskCodes(CollectorEtlNewNodeSaveReqVO vo, Map<Long, Long> definitionCopyVO) {
        if (vo == null || definitionCopyVO == null || definitionCopyVO.isEmpty()) {
            return;
        }

        // 1) 解析 taskDefinitionList
        String taskDefJson = vo.getTaskDefinitionList();
        if (taskDefJson != null && !taskDefJson.isEmpty()) {
            List<CollectorEtlNodeSaveReqVO> nodeList =
                    JSON.parseArray(taskDefJson, CollectorEtlNodeSaveReqVO.class);

            if (nodeList != null && !nodeList.isEmpty()) {
                for (CollectorEtlNodeSaveReqVO node : nodeList) {
                    node.setId(null);
                    // code 可能为字符串，需转成 Long 做映射
                    Long oldCode = JSONUtils.convertToLong(node.getCode());
                    if (oldCode != null) {
                        Long newCode = definitionCopyVO.get(oldCode);
                        if (newCode != null) {
                            node.setCode(String.valueOf(newCode));
                        }
                    }
                }
                vo.setTaskDefinitionList(JSON.toJSONString(nodeList));
            }
        }

        // 2) 解析 taskRelationJson
        String relJson = vo.getTaskRelationJson();
        if (relJson != null && !relJson.isEmpty()) {
            java.util.List<CollectorEtlTaskNodeRelRespVO> CollectorEtlTaskNodeRelRespVOList =
                    JSON.parseArray(relJson, CollectorEtlTaskNodeRelRespVO.class);

            List<ProcessTaskRelation> relList = new ArrayList<>();
            if (CollectorEtlTaskNodeRelRespVOList != null && !CollectorEtlTaskNodeRelRespVOList.isEmpty()) {
                for (CollectorEtlTaskNodeRelRespVO srcRel : CollectorEtlTaskNodeRelRespVOList) {
                    //调取映射子方法
                    ProcessTaskRelation rel = toProcessTaskRelation(srcRel, definitionCopyVO);
                    relList.add(rel);
                }
                vo.setTaskRelationJson(JSON.toJSONString(relList));
            }
        }
    }

    /**
     * 将 CollectorEtlTaskNodeRelRespVO → ProcessTaskRelation，并按 definitionCopyVO 重映射 pre/post code
     */
    private static ProcessTaskRelation toProcessTaskRelation(CollectorEtlTaskNodeRelRespVO src,
                                                             Map<Long, Long> definitionCopyVO) {
        ProcessTaskRelation rel = new ProcessTaskRelation();

        // 仅按你的要求映射这四个字段
        // preTaskCode
        String preCodeStr = src.getPreNodeCode();
        Long preOld = JSONUtils.convertToLong(preCodeStr);
        if (preOld != null && definitionCopyVO != null) {
            Long preNew = definitionCopyVO.get(preOld);
            if (preNew != null) {
                preCodeStr = String.valueOf(preNew);
            }
        }
        rel.setPreTaskCode(preCodeStr);

        // preTaskVersion
        rel.setPreTaskVersion(safeToInt(src.getPreNodeVersion()));

        // postTaskCode
        String postCodeStr = src.getPostNodeCode();
        Long postOld = JSONUtils.convertToLong(postCodeStr);
        if (postOld != null && definitionCopyVO != null) {
            Long postNew = definitionCopyVO.get(postOld);
            if (postNew != null) {
                postCodeStr = String.valueOf(postNew);
            }
        }
        rel.setPostTaskCode(postCodeStr);

        // postTaskVersion
        rel.setPostTaskVersion(safeToInt(src.getPostNodeVersion()));

        return rel;
    }

    private static int safeToInt(Long v) {
        return v == null ? 1 : (int) Math.min(Math.max(v, 1L), Integer.MAX_VALUE);
    }

    private String nextDsNodeCode(Long spaceCode) {
        DsNodeGenCodeRespDTO response = dsEtlNodeService.genCode(spaceCode);
        if (response == null || response.getData() == null || response.getData().isEmpty()) {
            throw new ServiceException("生成DolphinScheduler节点编码失败");
        }
        return String.valueOf(response.getData().get(0));
    }

    private TaskDefinition getDsTaskDefinition(ProcessDefinition definition, String nodeCode) {
        if (definition != null && definition.getTaskDefinitionList() != null) {
            for (TaskDefinition taskDefinition : definition.getTaskDefinitionList()) {
                if (StringUtils.equals(nodeCode, taskDefinition.getCode())) {
                    return taskDefinition;
                }
            }
        }
        throw new ServiceException("DolphinScheduler未返回节点: " + nodeCode);
    }

    private TaskDefinition getDsTaskDefinitionByType(ProcessDefinition definition, String taskType) {
        if (definition != null && definition.getTaskDefinitionList() != null) {
            for (TaskDefinition taskDefinition : definition.getTaskDefinitionList()) {
                if (StringUtils.equalsIgnoreCase(taskType, taskDefinition.getTaskType())) {
                    return taskDefinition;
                }
            }
        }
        throw new ServiceException("DolphinScheduler未返回类型为 " + taskType + " 的节点");
    }

    private TaskDefinition getDsPreviousTaskDefinition(ProcessDefinition definition, String nodeCode) {
        if (definition != null && definition.getTaskRelationList() != null) {
            for (ProcessTaskRelation relation : definition.getTaskRelationList()) {
                if (StringUtils.equals(nodeCode, relation.getPostTaskCode())
                        && !StringUtils.equals("0", relation.getPreTaskCode())) {
                    return getDsTaskDefinition(definition, relation.getPreTaskCode());
                }
            }
        }
        throw new ServiceException("DolphinScheduler未返回前置节点: " + nodeCode);
    }

    private TaskDefinition findDsNextTaskDefinition(ProcessDefinition definition, String nodeCode) {
        if (definition != null && definition.getTaskRelationList() != null) {
            for (ProcessTaskRelation relation : definition.getTaskRelationList()) {
                if (StringUtils.equals(nodeCode, relation.getPreTaskCode())) {
                    return getDsTaskDefinition(definition, relation.getPostTaskCode());
                }
            }
        }
        return null;
    }

    private TaskDefinition getDsNextTaskDefinition(ProcessDefinition definition, String nodeCode) {
        TaskDefinition taskDefinition = findDsNextTaskDefinition(definition, nodeCode);
        if (taskDefinition == null) {
            throw new ServiceException("DolphinScheduler未返回后置节点: " + nodeCode);
        }
        return taskDefinition;
    }

    private ProcessDefinition rebuildCopiedIncrementalTask(CollectorEtlTaskSaveReqVO task,
                                                            ProcessDefinition copiedDefinition) {
        TaskDefinition copiedFlinkxNode = getDsTaskDefinitionByType(copiedDefinition, "CHUNJUN");
        TaskDefinition copiedPrepareNode = getDsPreviousTaskDefinition(copiedDefinition, copiedFlinkxNode.getCode());
        TaskDefinition copiedCompleteNode = findDsNextTaskDefinition(copiedDefinition, copiedFlinkxNode.getCode());
        String conditionNodeCode = nextDsNodeCode(Long.valueOf(task.getSpaceCode()));
        String conditionNodeName = task.getName() + "-条件分支";
        String completeNodeCode = copiedCompleteNode == null
                ? nextDsNodeCode(Long.valueOf(task.getSpaceCode())) : copiedCompleteNode.getCode();
        String completeNodeName = task.getName() + "-状态回写";
        String failedCompleteNodeCode = nextDsNodeCode(Long.valueOf(task.getSpaceCode()));
        String failedCompleteNodeName = task.getName() + "-失败状态回写";

        DsTaskSaveReqDTO request = new DsTaskSaveReqDTO();
        request.setName(task.getName());
        request.setDescription(task.getDescription());
        request.setExecutionType(task.getExecutionType());
        request.setTaskDefinitionJson(TaskConverter.buildIncrementalFlinkxTaskDefinitionJson(
                copiedPrepareNode.getId(), task.getName() + "-增量边界准备", copiedPrepareNode.getCode(),
                copiedPrepareNode.getVersion(), copiedFlinkxNode.getId(), copiedFlinkxNode.getName(),
                copiedFlinkxNode.getCode(), copiedFlinkxNode.getVersion(),
                null, conditionNodeName, conditionNodeCode, 0,
                copiedCompleteNode == null ? null : copiedCompleteNode.getId(), completeNodeName,
                completeNodeCode, copiedCompleteNode == null ? 0 : copiedCompleteNode.getVersion(),
                null, failedCompleteNodeName, failedCompleteNodeCode, 0,
                incrementalCallbackUrl(incrementalPrepareUrl, task.getId()),
                incrementalCallbackUrl(incrementalCompleteUrl, task.getId(), 1),
                incrementalCallbackUrl(incrementalCompleteUrl, task.getId(), 0),
                task.getDraftJson(), getProjectWorkerGroup(task.getSpaceCode())));
        ProcessTaskRelation prepareRelation = getDsTaskRelation(copiedDefinition, copiedPrepareNode.getCode());
        ProcessTaskRelation flinkxRelation = getDsTaskRelation(copiedDefinition, copiedFlinkxNode.getCode());
        ProcessTaskRelation completeRelation = copiedCompleteNode == null
                ? null : getDsTaskRelation(copiedDefinition, copiedCompleteNode.getCode());
        request.setTaskRelationJson(TaskConverter.buildIncrementalFlinkxTaskRelationJson(
                prepareRelation.getId(), flinkxRelation.getId(),
                null, completeRelation == null ? null : completeRelation.getId(), null,
                copiedPrepareNode.getCode(), copiedPrepareNode.getVersion(),
                copiedFlinkxNode.getCode(), copiedFlinkxNode.getVersion(),
                conditionNodeCode, 0,
                completeNodeCode, copiedCompleteNode == null ? 0 : copiedCompleteNode.getVersion(),
                failedCompleteNodeCode, 0));
        List<Map<String, Object>> locations = JSONUtils.convertTaskDefinitionJson(task.getLocations());
        request.setLocations(TaskConverter.buildIncrementalFlinkxTaskLocationsJson(
                locations, copiedPrepareNode.getCode(), copiedFlinkxNode.getCode(), conditionNodeCode, completeNodeCode, failedCompleteNodeCode));

        DsTaskSaveRespDTO response = dsEtlTaskService.updateTask(request, task.getSpaceCode(), copiedDefinition.getCode());
        if (response == null || !response.getSuccess() || response.getData() == null) {
            throw new ServiceException("重建复制任务的DolphinScheduler回调节点失败: "
                    + (response == null ? "无响应" : response.getMsg()));
        }
        return response.getData();
    }

    private String resolveCopiedIncrementalInitialValue(CollectorEtlTaskUpdateQueryRespVO source,
                                                        CollectorEtlTaskExtDO origExt) {
        if (StringUtils.isNotBlank(origExt.getIncrementalInitialValue())) {
            return origExt.getIncrementalInitialValue();
        }
        try {
            Map<String, Object> mainArgs = TaskConverter.buildEtlTaskParams(
                    JSON.toJSONString(source.getTaskDefinitionList()),
                    new HashMap<>(), new HashMap<>(), new ArrayList<>());
            FlinkxIncrementalConfig config = TaskConverter.resolveFlinkxIncrementalConfig(mainArgs);
            if (config != null && StringUtils.isNotBlank(config.getIncrementalInitialValue())) {
                return config.getIncrementalInitialValue();
            }
        } catch (Exception e) {
            log.warn("从历史增量任务草稿解析初始游标失败，taskId={}", source.getId(), e);
        }
        return origExt.getIncrementalStartValue();
    }

    private ProcessTaskRelation getDsTaskRelation(ProcessDefinition definition, String postNodeCode) {
        if (definition != null && definition.getTaskRelationList() != null) {
            for (ProcessTaskRelation relation : definition.getTaskRelationList()) {
                if (StringUtils.equals(postNodeCode, relation.getPostTaskCode())) {
                    return relation;
                }
            }
        }
        throw new ServiceException("DolphinScheduler未返回节点关系: " + postNodeCode);
    }

    private void fillIncrementalExt(CollectorEtlTaskExtSaveReqVO taskExt, FlinkxIncrementalConfig config,
                                    String flinkxJobJson, ProcessDefinition definition,
                                    String prepareNodeCode, String prepareNodeName,
                                    String completeNodeCode, String completeNodeName) {
        if (config == null) {
            return;
        }
        TaskDefinition prepareNode = getDsTaskDefinition(definition, prepareNodeCode);
        ProcessTaskRelation prepareRelation = getDsTaskRelation(definition, prepareNodeCode);
        TaskDefinition completeNode = getDsTaskDefinition(definition, completeNodeCode);
        ProcessTaskRelation completeRelation = getDsTaskRelation(definition, completeNodeCode);
        taskExt.setFlinkxJobTemplateJson(flinkxJobJson);
        taskExt.setIncrementalType(config.getIncrementalType());
        taskExt.setSourceDatasourceId(config.getSourceDatasourceId());
        taskExt.setTargetDatasourceId(config.getTargetDatasourceId());
        taskExt.setSourceTableName(config.getSourceTableName());
        taskExt.setTargetTableName(config.getTargetTableName());
        taskExt.setSourceIncrementColumn(config.getSourceIncrementColumn());
        taskExt.setTargetIncrementColumn(config.getTargetIncrementColumn());
        taskExt.setIncrementalInitialValue(config.getIncrementalInitialValue());
        taskExt.setIncrementalTimeFormat(config.getIncrementalTimeFormat());
        taskExt.setPrepareNodeId(prepareNode.getId());
        taskExt.setPrepareNodeName(prepareNodeName);
        taskExt.setPrepareNodeCode(prepareNodeCode);
        taskExt.setPrepareNodeVersion(prepareNode.getVersion());
        taskExt.setPrepareRelationId(prepareRelation.getId());
        taskExt.setCompleteNodeId(completeNode.getId());
        taskExt.setCompleteNodeName(completeNodeName);
        taskExt.setCompleteNodeCode(completeNodeCode);
        taskExt.setCompleteNodeVersion(completeNode.getVersion());
        taskExt.setCompleteRelationId(completeRelation.getId());
    }

    private void fillIncrementalExt(CollectorEtlTaskExtDO taskExt, FlinkxIncrementalConfig config,
                                    String flinkxJobJson, ProcessDefinition definition,
                                    String prepareNodeCode, String prepareNodeName,
                                    String completeNodeCode, String completeNodeName) {
        if (config == null) {
            clearIncrementalExt(taskExt);
            return;
        }
        TaskDefinition prepareNode = getDsTaskDefinition(definition, prepareNodeCode);
        ProcessTaskRelation prepareRelation = getDsTaskRelation(definition, prepareNodeCode);
        TaskDefinition completeNode = getDsTaskDefinition(definition, completeNodeCode);
        ProcessTaskRelation completeRelation = getDsTaskRelation(definition, completeNodeCode);
        taskExt.setFlinkxJobTemplateJson(flinkxJobJson);
        taskExt.setIncrementalType(config.getIncrementalType());
        taskExt.setSourceDatasourceId(config.getSourceDatasourceId());
        taskExt.setTargetDatasourceId(config.getTargetDatasourceId());
        taskExt.setSourceTableName(config.getSourceTableName());
        taskExt.setTargetTableName(config.getTargetTableName());
        taskExt.setSourceIncrementColumn(config.getSourceIncrementColumn());
        taskExt.setTargetIncrementColumn(config.getTargetIncrementColumn());
        taskExt.setIncrementalInitialValue(config.getIncrementalInitialValue());
        taskExt.setIncrementalTimeFormat(config.getIncrementalTimeFormat());
        taskExt.setPrepareNodeId(prepareNode.getId());
        taskExt.setPrepareNodeName(prepareNodeName);
        taskExt.setPrepareNodeCode(prepareNodeCode);
        taskExt.setPrepareNodeVersion(prepareNode.getVersion());
        taskExt.setPrepareRelationId(prepareRelation.getId());
        taskExt.setCompleteNodeId(completeNode.getId());
        taskExt.setCompleteNodeName(completeNodeName);
        taskExt.setCompleteNodeCode(completeNodeCode);
        taskExt.setCompleteNodeVersion(completeNode.getVersion());
        taskExt.setCompleteRelationId(completeRelation.getId());
    }

    private void copyPublishedExt(CollectorEtlTaskExtSaveReqVO source, CollectorEtlTaskExtDO target) {
        target.setEtlTaskCode(source.getEtlTaskCode());
        target.setEtlTaskVersion(source.getEtlTaskVersion());
        target.setEtlNodeId(source.getEtlNodeId());
        target.setEtlNodeName(source.getEtlNodeName());
        target.setEtlNodeCode(source.getEtlNodeCode());
        target.setEtlNodeVersion(source.getEtlNodeVersion());
        target.setEtlRelationId(source.getEtlRelationId());
        target.setFlinkxJobJson(source.getFlinkxJobJson());
        target.setFlinkxJobTemplateJson(source.getFlinkxJobTemplateJson());
        target.setIncrementalType(source.getIncrementalType());
        target.setSourceDatasourceId(source.getSourceDatasourceId());
        target.setTargetDatasourceId(source.getTargetDatasourceId());
        target.setSourceTableName(source.getSourceTableName());
        target.setTargetTableName(source.getTargetTableName());
        target.setSourceIncrementColumn(source.getSourceIncrementColumn());
        target.setTargetIncrementColumn(source.getTargetIncrementColumn());
        target.setIncrementalInitialValue(source.getIncrementalInitialValue());
        target.setIncrementalTimeFormat(source.getIncrementalTimeFormat());
        target.setPrepareNodeId(source.getPrepareNodeId());
        target.setPrepareNodeName(source.getPrepareNodeName());
        target.setPrepareNodeCode(source.getPrepareNodeCode());
        target.setPrepareNodeVersion(source.getPrepareNodeVersion());
        target.setPrepareRelationId(source.getPrepareRelationId());
        target.setCompleteNodeId(source.getCompleteNodeId());
        target.setCompleteNodeName(source.getCompleteNodeName());
        target.setCompleteNodeCode(source.getCompleteNodeCode());
        target.setCompleteNodeVersion(source.getCompleteNodeVersion());
        target.setCompleteRelationId(source.getCompleteRelationId());
    }

    private void clearIncrementalExt(CollectorEtlTaskExtDO taskExt) {
        CollectorEtlTaskExtService.lambdaUpdate()
                .eq(CollectorEtlTaskExtDO::getId, taskExt.getId())
                .set(CollectorEtlTaskExtDO::getFlinkxJobTemplateJson, null)
                .set(CollectorEtlTaskExtDO::getIncrementalType, null)
                .set(CollectorEtlTaskExtDO::getSourceDatasourceId, null)
                .set(CollectorEtlTaskExtDO::getTargetDatasourceId, null)
                .set(CollectorEtlTaskExtDO::getSourceTableName, null)
                .set(CollectorEtlTaskExtDO::getTargetTableName, null)
                .set(CollectorEtlTaskExtDO::getSourceIncrementColumn, null)
                .set(CollectorEtlTaskExtDO::getTargetIncrementColumn, null)
                .set(CollectorEtlTaskExtDO::getIncrementalInitialValue, null)
                .set(CollectorEtlTaskExtDO::getIncrementalTimeFormat, null)
                .set(CollectorEtlTaskExtDO::getIncrementalStartValue, null)
                .set(CollectorEtlTaskExtDO::getIncrementalEndValue, null)
                .set(CollectorEtlTaskExtDO::getPrepareNodeId, null)
                .set(CollectorEtlTaskExtDO::getPrepareNodeName, null)
                .set(CollectorEtlTaskExtDO::getPrepareNodeCode, null)
                .set(CollectorEtlTaskExtDO::getPrepareNodeVersion, null)
                .set(CollectorEtlTaskExtDO::getPrepareRelationId, null)
                .update();
        taskExt.setFlinkxJobTemplateJson(null);
        taskExt.setIncrementalType(null);
        taskExt.setSourceDatasourceId(null);
        taskExt.setTargetDatasourceId(null);
        taskExt.setSourceTableName(null);
        taskExt.setTargetTableName(null);
        taskExt.setSourceIncrementColumn(null);
        taskExt.setTargetIncrementColumn(null);
        taskExt.setIncrementalInitialValue(null);
        taskExt.setIncrementalTimeFormat(null);
        taskExt.setIncrementalStartValue(null);
        taskExt.setIncrementalEndValue(null);
        taskExt.setPrepareNodeId(null);
        taskExt.setPrepareNodeName(null);
        taskExt.setPrepareNodeCode(null);
        taskExt.setPrepareNodeVersion(null);
        taskExt.setPrepareRelationId(null);
    }

    private void fillCompleteNodeExt(CollectorEtlTaskExtSaveReqVO taskExt, ProcessDefinition definition,
                                      String completeNodeCode, String completeNodeName) {
        if (completeNodeCode == null) {
            return;
        }
        TaskDefinition completeNode = getDsTaskDefinition(definition, completeNodeCode);
        ProcessTaskRelation completeRelation = getDsTaskRelation(definition, completeNodeCode);
        taskExt.setCompleteNodeId(completeNode.getId());
        taskExt.setCompleteNodeName(completeNodeName);
        taskExt.setCompleteNodeCode(completeNodeCode);
        taskExt.setCompleteNodeVersion(completeNode.getVersion());
        taskExt.setCompleteRelationId(completeRelation.getId());
    }

    private void fillCompleteNodeExt(CollectorEtlTaskExtDO taskExt, ProcessDefinition definition,
                                      String completeNodeCode, String completeNodeName) {
        if (completeNodeCode == null) {
            return;
        }
        TaskDefinition completeNode = getDsTaskDefinition(definition, completeNodeCode);
        ProcessTaskRelation completeRelation = getDsTaskRelation(definition, completeNodeCode);
        taskExt.setCompleteNodeId(completeNode.getId());
        taskExt.setCompleteNodeName(completeNodeName);
        taskExt.setCompleteNodeCode(completeNodeCode);
        taskExt.setCompleteNodeVersion(completeNode.getVersion());
        taskExt.setCompleteRelationId(completeRelation.getId());
    }

    private String incrementalCallbackUrl(String baseUrl, Long taskId) {
        return baseUrl + "/" + taskId;
    }

    private String incrementalCallbackUrl(String baseUrl, Long taskId, int status) {
        return incrementalCallbackUrl(baseUrl, taskId) + "?status=" + status;
    }

    private void createTaskLogSnapshotIfAbsent(CollectorEtlNewNodeSaveReqVO reqVO, ProcessDefinition data,
                                               String taskCode, String locations, Long fallbackDsId) {
        if (StringUtils.isNotBlank(reqVO.getDraftJson())) {
            Map<String, Object> draft = JSON.parseObject(reqVO.getDraftJson(), Map.class);
            if (draft.get("componentSnapshot") instanceof Map) {
                reqVO = JSON.parseObject(JSON.toJSONString(draft.get("componentSnapshot")), CollectorEtlNewNodeSaveReqVO.class);
                locations = JSON.toJSONString(reqVO.getLocations());
            }
        }
        CollectorEtlTaskSaveReqVO taskSaveReqVO = TaskConverter.convertToCollectorEtlTaskSaveReqVO(reqVO, data);
        Long dsId = data == null ? null : data.getId();
        if (dsId == null) {
            dsId = fallbackDsId;
        }
        taskSaveReqVO.setCode(taskCode);
        taskSaveReqVO.setDsId(dsId);
        taskSaveReqVO.setLocations(locations);
        CollectorEtlTaskLogSaveReqVO logSaveReqVO = TaskConverter.fromCollectorEtlTaskLogSaveReqVO(reqVO, taskSaveReqVO);
        logSaveReqVO.setCode(taskCode);
        logSaveReqVO.setDsId(dsId);
        logSaveReqVO.setLocations(locations);
        CollectorEtlTaskLogRespVO existing = getCollectorEtlTaskLogByRequest(logSaveReqVO);
        if (existing == null || existing.getId() == null) {
            iCollectorEtlTaskLogService.createCollectorEtlTaskLog(logSaveReqVO);
        }
    }

    private String getProjectWorkerGroup(Long spaceCode) {
        return getProjectWorkerGroup(spaceCode == null ? null : String.valueOf(spaceCode));
    }

    private String getProjectWorkerGroup(String spaceCode) {
        if (StringUtils.isEmpty(spaceCode)) {
            return "default";
        }
        return TaxonomySpaceApi.getWorkerGroupBySpaceCode(spaceCode);
    }

}
