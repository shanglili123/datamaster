

package com.datamaster.module.collector.service.etl.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson2.JSON;
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
import com.datamaster.common.enums.ExecuteType;
import com.datamaster.common.enums.TaskCatEnum;
import com.datamaster.common.enums.TaskComponentTypeEnum;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.JSONUtils;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.common.utils.uuid.IdUtils;
import com.datamaster.module.taxonomy.api.cat.ITaxonomyCatService;
import com.datamaster.module.taxonomy.api.cat.dto.TaxonomyDataDevCatReqDTO;
import com.datamaster.module.taxonomy.api.cat.dto.TaxonomyDataDevCatRespDTO;
import com.datamaster.module.taxonomy.api.cat.dto.TaxonomyTaskCatReqDTO;
import com.datamaster.module.taxonomy.api.cat.dto.TaxonomyTaskCatRespDTO;
import com.datamaster.module.taxonomy.api.service.cat.ITaxonomyDataDevCatApiService;
import com.datamaster.module.taxonomy.api.service.cat.ITaxonomyTaskCatApiService;
import com.datamaster.module.taxonomy.api.project.ITaxonomyProjectApi;
import com.datamaster.module.collector.api.etl.dto.CollectorEtlTaskRespDTO;
import com.datamaster.module.collector.api.service.etl.CollectorEtlTaskService;
import com.datamaster.module.collector.controller.admin.etl.vo.*;
import com.datamaster.module.collector.dal.dataobject.etl.*;
import com.datamaster.module.collector.dal.mapper.etl.CollectorEtlTaskMapper;
import com.datamaster.module.collector.service.etl.*;
import com.datamaster.module.collector.utils.IDGeneratorUtils;
import com.datamaster.flinkx.core.FlinkxEtlTaskConverter;
import com.datamaster.module.collector.utils.TaskConverter;
import com.datamaster.module.collector.utils.model.DsResource;
import com.datamaster.module.collector.utils.model.FlinkxIncrementalConfig;
import com.datamaster.mybatis.config.MasterDataSourceConfig;
import com.datamaster.mybatis.core.util.MyBatisUtils;
import com.datamaster.redis.service.IRedisService;
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
    private ITaxonomyCatService attCatService;
    @Resource
    private ITaxonomyTaskCatApiService iAttTaskCatApiService;
    @Resource
    private ITaxonomyDataDevCatApiService iAttDataDevCatApiService;
    @Resource
    private ICollectorEtlTaskExtService CollectorEtlTaskExtService;
    @Resource
    private IRedisService redisService;
    @Resource
    private ITaxonomyProjectApi taxonomyProjectApi;

    @Value("${ds.incremental_prepare_url:http://127.0.0.1:8080/col/etlTask/incremental/prepare}")
    private String incrementalPrepareUrl;
    @Value("${ds.incremental_complete_url:http://127.0.0.1:8080/col/etlTask/incremental/complete}")
    private String incrementalCompleteUrl;

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

    @Override
    public PageResult<CollectorEtlTaskRespVO> getCollectorEtlTaskPageList(CollectorEtlTaskPageReqVO CollectorEtlTask) {
        IPage<CollectorEtlTaskRespVO> mpPage = CollectorEtlTaskMapper.getCollectorEtlTaskPage(MyBatisUtils.buildPage(CollectorEtlTask), CollectorEtlTask);//BeanUtils.toBean(CollectorEtlTaskDOPageResult, CollectorEtlTaskRespVO.class);
        return new PageResult(mpPage.getRecords(), mpPage.getTotal());
    }

    @Override
    public Long createCollectorEtlTask(CollectorEtlTaskSaveReqVO createReqVO) {
        CollectorEtlTaskDO CollectorEtlTaskDO = BeanUtils.toBean(createReqVO, CollectorEtlTaskDO.class);
        if (StringUtils.isNotEmpty(CollectorEtlTaskDO.getCatCode()) && StringUtils.isNotEmpty(createReqVO.getType())) {
            CollectorEtlTaskDO.setCatId(attCatService.getCatIdByTableNameAndCatCode(TaskCatEnum.findEnumByType(createReqVO.getType()).toString(), CollectorEtlTaskDO.getCatCode()));
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
            updateReqVO.setCatId(attCatService.getCatIdByTableNameAndCatCode(TaskCatEnum.findEnumByType(updateReqVO.getType()).toString(), updateReqVO.getCatCode()));
        }
        return CollectorEtlTaskMapper.updateById(updateObj);
    }

    @Override
    public int removeCollectorEtlTask(Collection<Long> idList) {
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
                dsEtlTaskService.deleteTask(CollectorEtlTaskDO.getProjectCode(), dsTaskCode);
            }
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
        DsNodeGenCodeRespDTO dsNodeGenCodeRespDTO = dsEtlNodeService.genCode(CollectorEtlNewNodeSaveReqVO.getProjectCode());
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

        //判断是否是离线任务 是需要获取扩展信息的任务编码进行接口调用
        if (StringUtils.equals("1", type)) {
            //获取扩展信息
            CollectorEtlTaskExtDO taskExt = CollectorEtlTaskExtService.getByTaskId(Long.parseLong(CollectorEtlNewNodeSaveReqVO.getIdStr()));
            if (taskExt == null) {
                throw new ServiceException("暂无数据！");
            }
            CollectorEtlTaskDO.setCode(taskExt.getEtlTaskCode());
        }

        // 下线操作
        if (StringUtils.equals("0", CollectorEtlNewNodeSaveReqVO.getReleaseState())) {
            DsStatusRespDTO dsStatusRespDTO = dsEtlTaskService.releaseTask("OFFLINE", String.valueOf(CollectorEtlTaskDO.getProjectCode()), CollectorEtlTaskDO.getCode());
            if (dsStatusRespDTO == null || !dsStatusRespDTO.getSuccess()) {
                throw new ServiceException("发布或下线任务，失败！");
            }

            // 更新任务状态
            if (!StringUtils.equals("-2", CollectorEtlTaskDO.getStatus()) && !StringUtils.equals("-3", CollectorEtlTaskDO.getStatus())) {
                updateTaskStatus(CollectorEtlTaskDO.getId(), CollectorEtlNewNodeSaveReqVO.getReleaseState());
            } else {
                updateTaskStatus(CollectorEtlTaskDO.getId(), "-2");
            }
            return new HashMap<>();
        }

        // 上线操作
        DsStatusRespDTO dsStatusRespDTO = dsEtlTaskService.releaseTask("ONLINE", String.valueOf(CollectorEtlTaskDO.getProjectCode()), CollectorEtlTaskDO.getCode());
        String responseMsg = dsStatusRespDTO.getMsg();
        if (responseMsg.contains("SubWorkflowDefinition") && responseMsg.contains("is not online")) {
            throw new RuntimeException("存在未上线的子工作流，请先将所有子工作流上线");
        }
        if (dsStatusRespDTO == null || !dsStatusRespDTO.getSuccess()) {
            throw new ServiceException("发布任务失败！");
        }

        // 更新任务状态
        if (!StringUtils.equals("-2", CollectorEtlTaskDO.getStatus()) && !StringUtils.equals("-3", CollectorEtlTaskDO.getStatus())) {
            updateTaskStatus(CollectorEtlTaskDO.getId(), CollectorEtlNewNodeSaveReqVO.getReleaseState());
        } else {
            updateTaskStatus(CollectorEtlTaskDO.getId(), "-3");
        }

        return null;
    }

    @Override
    public Map<String, Object> updateReleaseSchedule(CollectorEtlNewNodeSaveReqVO CollectorEtlNewNodeSaveReqVO) {
        CollectorEtlTaskDO CollectorEtlTaskDO = CollectorEtlTaskMapper.selectById(CollectorEtlNewNodeSaveReqVO.getIdStr());
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
                DsStatusRespDTO dsStatusRespDTO1 = iDsEtlSchedulerService.offlineScheduler(CollectorEtlTaskDO.getProjectCode(), CollectorEtlSchedulerById.getDsId());
                if (!dsStatusRespDTO1.getData()) {
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

        DsStatusRespDTO dsStatusRespDTO1 = iDsEtlSchedulerService.onlineScheduler(CollectorEtlTaskDO.getProjectCode(), CollectorEtlSchedulerSaveReqVO.getDsId());
        if (!dsStatusRespDTO1.getData()) {
            throw new ServiceException("上线调度器，失败！");
        }

        // 更新调度器
        iCollectorEtlSchedulerService.updateCollectorEtlScheduler(CollectorEtlSchedulerSaveReqVO);
        return null;
    }

    @Override
    public CollectorEtlTaskSaveReqVO updateEtlTask(CollectorEtlNewNodeSaveReqVO CollectorEtlNewNodeSaveReqVO) {
        CollectorEtlTaskDO CollectorEtlTaskDO = CollectorEtlTaskMapper.selectById(CollectorEtlNewNodeSaveReqVO.getIdStr());
        if (StringUtils.equals("1", CollectorEtlTaskDO.getStatus()) || StringUtils.equals("-3", CollectorEtlTaskDO.getStatus())) {
            throw new ServiceException("上线任务，不允许修改，请先下线！");
        }

        return saveEtlTaskLocal(CollectorEtlNewNodeSaveReqVO);
    }

    @Override
    public Map<String, Object> updateReleaseTask(CollectorEtlNewNodeSaveReqVO CollectorEtlNewNodeSaveReqVO) {
        CollectorEtlTaskDO CollectorEtlTaskDO = CollectorEtlTaskMapper.selectById(CollectorEtlNewNodeSaveReqVO.getIdStr());
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
                DsStatusRespDTO dsStatusRespDTO = dsEtlTaskService.releaseTask("OFFLINE", String.valueOf(CollectorEtlTaskDO.getProjectCode()), CollectorEtlTaskDO.getCode());
                if (dsStatusRespDTO == null || !dsStatusRespDTO.getSuccess()) {
                    throw new ServiceException("发布或下线任务，失败！");
                }
                DsStatusRespDTO dsStatusRespDTO1 = iDsEtlSchedulerService.offlineScheduler(CollectorEtlTaskDO.getProjectCode(), CollectorEtlSchedulerById.getDsId());
                if (!dsStatusRespDTO1.getData()) {
                    throw new ServiceException("下线调度器，失败！");
                }
            }
            // 更新任务状态
            if (!StringUtils.equals("-2", CollectorEtlTaskDO.getStatus()) && !StringUtils.equals("-3", CollectorEtlTaskDO.getStatus())) {
                updateTaskStatus(CollectorEtlTaskDO.getId(), CollectorEtlNewNodeSaveReqVO.getReleaseState());
            } else {
                updateTaskStatus(CollectorEtlTaskDO.getId(), "-2");
            }
        }

        // 上线操作
        DsStatusRespDTO dsStatusRespDTO = dsEtlTaskService.releaseTask("ONLINE", String.valueOf(CollectorEtlTaskDO.getProjectCode()), CollectorEtlTaskDO.getCode());
        String responseMsg = dsStatusRespDTO.getMsg();
        if (responseMsg.contains("SubWorkflowDefinition") && responseMsg.contains("is not online")) {
            throw new RuntimeException("存在未上线的子工作流，请先将所有子工作流上线");
        }
        if (dsStatusRespDTO == null || !dsStatusRespDTO.getSuccess()) {
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

        DsStatusRespDTO dsStatusRespDTO1 = iDsEtlSchedulerService.onlineScheduler(CollectorEtlTaskDO.getProjectCode(), CollectorEtlSchedulerSaveReqVO.getDsId());
        if (!dsStatusRespDTO1.getData()) {
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

    // 创建或更新调度器
    private DsSchedulerRespDTO createOrUpdateScheduler(CollectorEtlSchedulerDO CollectorEtlSchedulerById, CollectorEtlTaskDO CollectorEtlTaskDO) {
        DsSchedulerSaveReqDTO dsSchedulerSaveReqDTO = TaskConverter.createSchedulerRequest(CollectorEtlSchedulerById.getCronExpression(), CollectorEtlTaskDO.getCode(), getProjectWorkerGroup(CollectorEtlTaskDO.getProjectCode()));
        DsSchedulerRespDTO dsSchedulerRespDTO = iDsEtlSchedulerService.saveScheduler(dsSchedulerSaveReqDTO, String.valueOf(CollectorEtlTaskDO.getProjectCode()));
        if (dsSchedulerRespDTO == null || !dsSchedulerRespDTO.getSuccess()) {
            DsSchedulerRespDTO byTaskCode = iDsEtlSchedulerService.getByTaskCode(String.valueOf(CollectorEtlTaskDO.getProjectCode()), CollectorEtlTaskDO.getCode());
            if (byTaskCode == null || !byTaskCode.getSuccess()) {
                throw new ServiceException("创建调度器，失败！");
            }
            Schedule data = byTaskCode.getData();
            DsSchedulerUpdateReqDTO schedulerUpdateRequest = TaskConverter.createSchedulerUpdateRequest(data.getId(), CollectorEtlSchedulerById.getCronExpression(), CollectorEtlTaskDO.getCode(), getProjectWorkerGroup(CollectorEtlTaskDO.getProjectCode()));
            dsSchedulerRespDTO = iDsEtlSchedulerService.updateScheduler(schedulerUpdateRequest, String.valueOf(CollectorEtlTaskDO.getProjectCode()));
            if (dsSchedulerRespDTO == null || !dsSchedulerRespDTO.getSuccess()) {
                throw new ServiceException("更新调度器，失败！");
            }
        }
        return dsSchedulerRespDTO;
    }

    // 更新现有调度器
    private DsSchedulerRespDTO updateExistingScheduler(CollectorEtlSchedulerDO CollectorEtlSchedulerById, CollectorEtlTaskDO CollectorEtlTaskDO) {
        DsSchedulerUpdateReqDTO schedulerUpdateRequest = TaskConverter.createSchedulerUpdateRequest(CollectorEtlSchedulerById.getDsId(), CollectorEtlSchedulerById.getCronExpression(), CollectorEtlTaskDO.getCode(), getProjectWorkerGroup(CollectorEtlTaskDO.getProjectCode()));
        DsSchedulerRespDTO dsSchedulerRespDTO = iDsEtlSchedulerService.updateScheduler(schedulerUpdateRequest, String.valueOf(CollectorEtlTaskDO.getProjectCode()));
        if (dsSchedulerRespDTO == null || !dsSchedulerRespDTO.getSuccess()) {
            DsSchedulerRespDTO byTaskCode = iDsEtlSchedulerService.getByTaskCode(String.valueOf(CollectorEtlTaskDO.getProjectCode()), CollectorEtlTaskDO.getCode());
            if (byTaskCode == null || !byTaskCode.getSuccess()) {
                DsSchedulerSaveReqDTO dsSchedulerSaveReqDTO = TaskConverter.createSchedulerRequest(CollectorEtlSchedulerById.getCronExpression(), CollectorEtlTaskDO.getCode(), getProjectWorkerGroup(CollectorEtlTaskDO.getProjectCode()));
                DsSchedulerRespDTO saveScheduler = iDsEtlSchedulerService.saveScheduler(dsSchedulerSaveReqDTO, String.valueOf(CollectorEtlTaskDO.getProjectCode()));
                if (saveScheduler == null || !saveScheduler.getSuccess()) {
                    throw new ServiceException("创建调度器，失败！");
                }
                return saveScheduler;
            }
            Schedule data = byTaskCode.getData();
            DsSchedulerUpdateReqDTO updateRequest = TaskConverter.createSchedulerUpdateRequest(data.getId(), CollectorEtlSchedulerById.getCronExpression(), CollectorEtlTaskDO.getCode(), getProjectWorkerGroup(CollectorEtlTaskDO.getProjectCode()));
            dsSchedulerRespDTO = iDsEtlSchedulerService.updateScheduler(updateRequest, String.valueOf(CollectorEtlTaskDO.getProjectCode()));
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
            DsSchedulerUpdateReqDTO schedulerUpdateRequest = TaskConverter.createSchedulerUpdateRequest(CollectorEtlSchedulerById.getDsId(), CollectorEtlNewNodeSaveReqVO.getCrontab(), dsTaskCode, getProjectWorkerGroup(CollectorEtlTaskDO.getProjectCode()));
            dsSchedulerRespDTO = iDsEtlSchedulerService.updateScheduler(schedulerUpdateRequest, String.valueOf(CollectorEtlTaskDO.getProjectCode()));
            if (dsSchedulerRespDTO == null || !dsSchedulerRespDTO.getSuccess()) {
                DsSchedulerRespDTO byTaskCode = iDsEtlSchedulerService.getByTaskCode(String.valueOf(CollectorEtlTaskDO.getProjectCode()), dsTaskCode);
                if (byTaskCode != null && byTaskCode.getSuccess()) {
                    Schedule data = byTaskCode.getData();
                    DsSchedulerUpdateReqDTO updateRequest = TaskConverter.createSchedulerUpdateRequest(data.getId(), CollectorEtlNewNodeSaveReqVO.getCrontab(), dsTaskCode, getProjectWorkerGroup(CollectorEtlTaskDO.getProjectCode()));
                    dsSchedulerRespDTO = iDsEtlSchedulerService.updateScheduler(updateRequest, String.valueOf(CollectorEtlTaskDO.getProjectCode()));
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
        // 单独查询责任人名称
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

        //获取最后一次执行的实例
        CollectorEtlTaskInstanceDO CollectorEtlTaskInstanceDO = CollectorEtlTaskInstanceService.getLastTaskInstanceByTaskCode(bean.getCode());
        if (CollectorEtlTaskInstanceDO != null) {
            bean.setLastExecuteTime(CollectorEtlTaskInstanceDO.getStartTime());
            bean.setLastExecuteStatus(CollectorEtlTaskInstanceDO.getStatus());
        }
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
                            //增量字段
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
        if (!StringUtils.equals("1", CollectorEtlTaskDO.getStatus())) {
            CollectorEtlNewNodeSaveReqVO nodeSaveReqVO = new CollectorEtlNewNodeSaveReqVO();
            nodeSaveReqVO.setIdStr(String.valueOf(id));
            nodeSaveReqVO.setReleaseState("1");
            this.updateReleaseJobTask(nodeSaveReqVO);
//            return error("任务状态错误，请刷新后重试！");
        }
        //1：离线任务 2：实时任务 3：数据开发任务 4：作业任务
        String type = CollectorEtlTaskDO.getType();

        //判断是否是离线任务 是需要获取扩展信息的任务编码进行接口调用
        if (StringUtils.equals("1", type)) {
            //获取扩展信息
            CollectorEtlTaskExtDO taskExt = CollectorEtlTaskExtService.getByTaskId(CollectorEtlTaskDO.getId());
            if (taskExt == null) {
                throw new ServiceException("暂无数据！");
            }
            CollectorEtlTaskDO.setCode(taskExt.getEtlTaskCode());
        }


        DsStartTaskReqDTO dsStartTaskReqDTO = TaskConverter.createDsStartTaskReqDTO(CollectorEtlTaskDO.getCode(), getProjectWorkerGroup(CollectorEtlTaskDO.getProjectCode()));

        DsStatusRespDTO dsStatusRespDTO = dsEtlTaskService.startTask(dsStartTaskReqDTO, CollectorEtlTaskDO.getProjectCode());

        return dsStatusRespDTO.getSuccess() ? success() : error(dsStatusRespDTO.getMsg());
    }


    @Override
    public List<CollectorEtlTaskTreeRespVO> getCollectorEtlTaskListTree(CollectorEtlTaskPageReqVO reqVO) {
        MPJLambdaWrapper<CollectorEtlTaskDO> wrapper = new MPJLambdaWrapper<>();
        wrapper.selectAll(CollectorEtlTaskDO.class)
                .ne(CollectorEtlTaskDO::getStatus, "-2")
                .ne(CollectorEtlTaskDO::getStatus, "-3")
                .eq(reqVO.getProjectId() != null, CollectorEtlTaskDO::getProjectId, reqVO.getProjectId())
                .eq(StringUtils.isNotBlank(reqVO.getProjectCode()), CollectorEtlTaskDO::getProjectCode, reqVO.getProjectCode())
                .ne(CollectorEtlTaskDO::getType, "4");
        List<CollectorEtlTaskDO> CollectorEtlTaskDOS = CollectorEtlTaskMapper.selectList(wrapper);
        List<CollectorEtlTaskRespVO> CollectorEtlTaskRespVOList = BeanUtils.toBean(CollectorEtlTaskDOS, CollectorEtlTaskRespVO.class);

        List<TaxonomyTaskCatRespDTO> attTaskCatApiList = iAttTaskCatApiService.getAttTaskCatApiList(new TaxonomyTaskCatReqDTO());
        List<TaxonomyDataDevCatRespDTO> attDataDevCatApiList = iAttDataDevCatApiService.getAttDataDevCatApiList(new TaxonomyDataDevCatReqDTO());

        List<CollectorEtlTaskTreeRespVO> result = new ArrayList<>();
        CollectorEtlTaskTreeRespVO builtTaskCatTree = buildTaskCatTree(CollectorEtlTaskRespVOList, attTaskCatApiList);
        CollectorEtlTaskTreeRespVO builtTaskDevCaTree = buildTaskDevCaTree(CollectorEtlTaskRespVOList, attDataDevCatApiList);


        result.add(builtTaskCatTree);
        result.add(builtTaskDevCaTree);
        return result;
    }

    private CollectorEtlTaskTreeRespVO buildTaskDevCaTree(List<CollectorEtlTaskRespVO> CollectorEtlTaskRespVOList, List<TaxonomyDataDevCatRespDTO> attDataDevCatApiList) {

        // 1. 构造顶级目录节点
        CollectorEtlTaskTreeRespVO root = new CollectorEtlTaskTreeRespVO();
        root.setId(IdUtils.generateArtificialId());
        root.setTreeId(IdUtils.generateArtificialId());
        root.setLabel("数据开发");
        root.setChildren(new ArrayList<>());

        // 2. 整理类目列表：先将每个 AttTaskCatRespDTO 转换为 CollectorEtlTaskTreeRespVO 节点，并放入 map（key 为类目 id）
        Map<Long, CollectorEtlTaskTreeRespVO> catNodeMap = new HashMap<>();
        for (TaxonomyDataDevCatRespDTO cat : attDataDevCatApiList) {
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
        for (TaxonomyDataDevCatRespDTO cat : attDataDevCatApiList) {
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
                taskNode.setProjectId(task.getProjectId());
                taskNode.setProjectCode(task.getProjectCode());
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
                                                 List<TaxonomyTaskCatRespDTO> attTaskCatApiList) {
        // 1. 构造顶级目录节点
        CollectorEtlTaskTreeRespVO root = new CollectorEtlTaskTreeRespVO();
        // 重新生成一个 id
        root.setTreeId(IdUtils.generateArtificialId());
        root.setId(IdUtils.generateArtificialId());
        root.setLabel("数据集成");
        root.setChildren(new ArrayList<>());

        // 2. 整理类目列表：先将每个 AttTaskCatRespDTO 转换为 CollectorEtlTaskTreeRespVO 节点，并放入 map（key 为类目 id）
        Map<Long, CollectorEtlTaskTreeRespVO> catNodeMap = new HashMap<>();
        for (TaxonomyTaskCatRespDTO cat : attTaskCatApiList) {
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
        for (TaxonomyTaskCatRespDTO cat : attTaskCatApiList) {
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
                taskNode.setProjectId(task.getProjectId());
                taskNode.setProjectCode(task.getProjectCode());
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
    public int checkTaskIdInDatasource(List<Long> datasourceIdList, List<Long> projectIdList) {
        return CollectorEtlTaskMapper.checkTaskIdInDatasource(datasourceIdList, projectIdList);
    }

    @Override
    public int checkTaskIdInAsset(List<Long> assetIdList) {
        return CollectorEtlTaskMapper.checkTaskIdInAsset(assetIdList);
    }

    @Override
    public CollectorEtlNewNodeSaveReqVO createEtlTaskFront(CollectorEtlNewNodeSaveReqVO CollectorEtlNewNodeSaveReqVO) {

        //生成任务编码(本地唯一ID，发布时替换为DS编码)
        String taskCode = String.valueOf(IdUtils.generateArtificialId());

        CollectorEtlTaskSaveReqVO createReqVO = new CollectorEtlTaskSaveReqVO();
        createReqVO.setName(CollectorEtlNewNodeSaveReqVO.getName());
        createReqVO.setType(CollectorEtlNewNodeSaveReqVO.getType());
        createReqVO.setCatCode(CollectorEtlNewNodeSaveReqVO.getCatCode());
        createReqVO.setProjectId(CollectorEtlNewNodeSaveReqVO.getProjectId());
        createReqVO.setProjectCode(String.valueOf(CollectorEtlNewNodeSaveReqVO.getProjectCode()));
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

        CollectorEtlTaskSaveReqVO taskSaveReqVO = BeanUtils.toBean(taskDO, CollectorEtlTaskSaveReqVO.class);
        taskSaveReqVO.setName(reqVO.getName());
        taskSaveReqVO.setLocations(JSON.toJSONString(reqVO.getLocations()));
        taskSaveReqVO.setDescription(reqVO.getDescription());
        taskSaveReqVO.setStatus("0");
        taskSaveReqVO.setDraftJson(reqVO.getDraftJson());

        //处理节点数据
        List<CollectorEtlNodeSaveReqVO> nodeList = JSON.parseArray(reqVO.getTaskDefinitionList(), CollectorEtlNodeSaveReqVO.class);
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
            createReqVO.setProjectId(reqVO.getProjectId());
            createReqVO.setProjectCode(String.valueOf(reqVO.getProjectCode()));
            createReqVO.setParameters(JSON.toJSONString(createReqVO.getTaskParams()));

            CollectorEtlNodeLogDO nodeCodeAndVersion = iCollectorEtlNodeLogService.getByNodeCodeAndVersion(
                    createReqVO.getCode(), createReqVO.getVersion());
            if (nodeCodeAndVersion == null) {
                createReqVO.setCreatorId(reqVO.getCreatorId());
                createReqVO.setCreateBy(reqVO.getCreateBy());
                createReqVO.setCreateTime(reqVO.getCreateTime());
                newTaskDefinitionLogs.add(createReqVO);
                continue;
            } else {
                if (StringUtils.equals(TaskComponentTypeEnum.DB_READER.getCode(), String.valueOf(createReqVO.getTaskParams().get("type"))) &&
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
        reqVO = fillPublishPayload(reqVO);
        Long taskId = resolveTaskId(reqVO);
        CollectorEtlTaskDO taskDO = CollectorEtlTaskMapper.selectById(taskId);
        if (taskDO == null) {
            throw new ServiceException("任务不存在");
        }
        if (StringUtils.equals("1", taskDO.getStatus()) || StringUtils.equals("-3", taskDO.getStatus())) {
            throw new ServiceException("任务已发布，无需重复发布");
        }
        if (StringUtils.isBlank(reqVO.getTaskDefinitionList())) {
            throw new ServiceException("任务配置未完成，请先保存任务配置");
        }

        CollectorEtlTaskExtDO taskExt = CollectorEtlTaskExtService.getByTaskId(taskDO.getId());
        boolean hasPublishedExt = taskExt != null && StringUtils.isNotEmpty(taskExt.getEtlTaskCode());
        ProcessDefinition existingDsDefinition = hasPublishedExt ? null
                : dsEtlTaskService.getTaskByName(String.valueOf(reqVO.getProjectCode()), reqVO.getName());
        boolean isFirstPublish = !hasPublishedExt;
        boolean createDsDefinition = isFirstPublish
                && (existingDsDefinition == null || StringUtils.isEmpty(existingDsDefinition.getCode()));

        String nodeCode;
        String nodeName;
        if (isFirstPublish) {
            DsNodeGenCodeRespDTO dsNodeGenCodeRespDTO = dsEtlNodeService.genCode(reqVO.getProjectCode());
            nodeCode = String.valueOf(dsNodeGenCodeRespDTO.getData().get(0));
            nodeName = reqVO.getName() + "-" + DateUtil.today();
        } else {
            nodeCode = taskExt.getEtlNodeCode();
            nodeName = StringUtils.isNotBlank(taskExt.getEtlNodeName()) ? taskExt.getEtlNodeName() : reqVO.getName() + "-" + DateUtil.today();
        }

        DsTaskSaveReqDTO dsTaskSaveReqDTO = new DsTaskSaveReqDTO();
        dsTaskSaveReqDTO.setName(reqVO.getName());
        dsTaskSaveReqDTO.setDescription(reqVO.getDescription());
        dsTaskSaveReqDTO.setExecutionType(reqVO.getExecutionType());

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

        if (isFirstPublish) {
            //首次发布：本地没有已发布扩展；如果DS中已有同名定义，则复用定义编码走更新
            if (existingDsDefinition != null && StringUtils.isNotEmpty(existingDsDefinition.getCode())) {
                taskCode = existingDsDefinition.getCode();
            } else {
                DsNodeGenCodeRespDTO dsTaskGenCodeRespDTO = dsEtlNodeService.genCode(reqVO.getProjectCode());
                taskCode = String.valueOf(dsTaskGenCodeRespDTO.getData().get(0));
            }

            taskInfo.put("projectCode", reqVO.getProjectCode());
            taskInfo.put("taskCode", taskCode);
            taskInfo.put("taskVersion", existingDsDefinition == null ? 1 : existingDsDefinition.getVersion() + 1);
            taskInfo.put("name", reqVO.getName());

            Map<String, Object> mainArgs = TaskConverter.buildEtlTaskParams(reqVO.getTaskDefinitionList(), new HashMap<>(), taskInfo, resourceList);

            if (isFlinkx) {
                flinkxJobJson = FlinkxEtlTaskConverter.convertToFlinkxJobJson(mainArgs);
                streamingFlinkx = TaskConverter.isStreamingFlinkxJob(flinkxJobJson);
                incrementalConfig = TaskConverter.resolveFlinkxIncrementalConfig(mainArgs);
                if (incrementalConfig != null) {
                    prepareNodeCode = nextDsNodeCode(reqVO.getProjectCode());
                    completeNodeCode = nextDsNodeCode(reqVO.getProjectCode());
                    taskDefinition = TaskConverter.buildIncrementalFlinkxTaskDefinitionJson(
                             null, prepareNodeName, prepareNodeCode, 0,
                             null, nodeName, nodeCode, 0,
                             null, completeNodeName, completeNodeCode, 0,
                             incrementalCallbackUrl(incrementalPrepareUrl, taskDO.getId()),
                             incrementalCallbackUrl(incrementalCompleteUrl, taskDO.getId()), reqVO.getDraftJson(),
                             getProjectWorkerGroup(reqVO.getProjectCode()));
                } else {
                    taskDefinition = TaskConverter.buildEtlTaskDefinitionJsonFlinkx(null, nodeName, nodeCode, 0, flinkxJobJson, reqVO.getDraftJson(), getProjectWorkerGroup(reqVO.getProjectCode()));
                }
            } else {
                taskDefinition = TaskConverter.buildEtlTaskDefinitionJson(null, nodeName, nodeCode, 0, mainArgs, reqVO.getDraftJson(), getProjectWorkerGroup(reqVO.getProjectCode()));
            }

            taskRelation = incrementalConfig == null
                    ? TaskConverter.buildEtlTaskRelationJson(null, nodeCode)
                    : TaskConverter.buildIncrementalFlinkxTaskRelationJson(
                            null, null, null, prepareNodeCode, nodeCode, completeNodeCode);
            locations = incrementalConfig == null
                    ? TaskConverter.buildEtlTaskLocationsJson(reqVO.getLocations(), nodeCode)
                    : TaskConverter.buildIncrementalFlinkxTaskLocationsJson(
                            reqVO.getLocations(), prepareNodeCode, nodeCode, completeNodeCode);

            dsTaskSaveReqDTO.setTaskDefinitionJson(taskDefinition);
            dsTaskSaveReqDTO.setTaskRelationJson(taskRelation);
            dsTaskSaveReqDTO.setLocations(locations);

            //创建或恢复更新DS任务
            DsTaskSaveRespDTO task = createDsDefinition
                    ? dsEtlTaskService.createTask(dsTaskSaveReqDTO, reqVO.getProjectCode())
                    : updateDsTaskAllowingOnline(dsTaskSaveReqDTO, String.valueOf(reqVO.getProjectCode()), taskCode);
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
            }
            fillIncrementalExt(extSaveReqVO, incrementalConfig, flinkxJobJson, data,
                    prepareNodeCode, prepareNodeName, completeNodeCode, completeNodeName);
            if (taskExt != null) {
                copyPublishedExt(extSaveReqVO, taskExt);
                if (incrementalConfig == null) {
                    clearIncrementalExt(taskExt);
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

            taskInfo.put("projectCode", reqVO.getProjectCode());
            taskInfo.put("taskCode", taskCode);
            taskInfo.put("taskVersion", taskExt.getEtlTaskVersion() != null ? taskExt.getEtlTaskVersion() + 1 : 1);
            taskInfo.put("name", reqVO.getName());

            Map<String, Object> mainArgs = TaskConverter.buildEtlTaskParams(reqVO.getTaskDefinitionList(), new HashMap<>(), taskInfo, resourceList);

            if (isFlinkx) {
                flinkxJobJson = FlinkxEtlTaskConverter.convertToFlinkxJobJson(mainArgs);
                streamingFlinkx = TaskConverter.isStreamingFlinkxJob(flinkxJobJson);
                incrementalConfig = TaskConverter.resolveFlinkxIncrementalConfig(mainArgs);
                if (incrementalConfig != null) {
                    prepareNodeCode = StringUtils.isNotBlank(taskExt.getPrepareNodeCode())
                            ? taskExt.getPrepareNodeCode() : nextDsNodeCode(reqVO.getProjectCode());
                    completeNodeCode = StringUtils.isNotBlank(taskExt.getCompleteNodeCode())
                            ? taskExt.getCompleteNodeCode() : nextDsNodeCode(reqVO.getProjectCode());
                    taskDefinition = TaskConverter.buildIncrementalFlinkxTaskDefinitionJson(
                             taskExt.getPrepareNodeId(), prepareNodeName, prepareNodeCode,
                             taskExt.getPrepareNodeVersion(), taskExt.getEtlNodeId(), nodeName,
                             nodeCode, taskExt.getEtlNodeVersion(), taskExt.getCompleteNodeId(),
                             completeNodeName, completeNodeCode, taskExt.getCompleteNodeVersion(),
                             incrementalCallbackUrl(incrementalPrepareUrl, taskDO.getId()),
                             incrementalCallbackUrl(incrementalCompleteUrl, taskDO.getId()), reqVO.getDraftJson(),
                             getProjectWorkerGroup(reqVO.getProjectCode()));
                } else {
                    taskDefinition = TaskConverter.buildEtlTaskDefinitionJsonFlinkx(taskExt.getEtlNodeId(), nodeName, nodeCode, nodeVersion, flinkxJobJson, reqVO.getDraftJson(), getProjectWorkerGroup(reqVO.getProjectCode()));
                }
            } else {
                taskDefinition = TaskConverter.buildEtlTaskDefinitionJson(taskExt.getEtlNodeId(), nodeName, nodeCode, nodeVersion, mainArgs, reqVO.getDraftJson(), getProjectWorkerGroup(reqVO.getProjectCode()));
            }

            taskRelation = incrementalConfig == null
                    ? TaskConverter.buildEtlTaskRelationJson(taskExt.getEtlRelationId(), nodeCode, nodeVersion)
                    : TaskConverter.buildIncrementalFlinkxTaskRelationJson(
                            taskExt.getPrepareRelationId(), taskExt.getEtlRelationId(), taskExt.getCompleteRelationId(),
                             prepareNodeCode, taskExt.getPrepareNodeVersion(),
                             nodeCode, taskExt.getEtlNodeVersion(),
                             completeNodeCode, taskExt.getCompleteNodeVersion());
            locations = incrementalConfig == null
                    ? TaskConverter.buildEtlTaskLocationsJson(reqVO.getLocations(), nodeCode)
                    : TaskConverter.buildIncrementalFlinkxTaskLocationsJson(
                            reqVO.getLocations(), prepareNodeCode, nodeCode, completeNodeCode);

            dsTaskSaveReqDTO.setTaskDefinitionJson(taskDefinition);
            dsTaskSaveReqDTO.setTaskRelationJson(taskRelation);
            dsTaskSaveReqDTO.setLocations(locations);

            //更新DS任务
            DsTaskSaveRespDTO task = updateDsTaskAllowingOnline(dsTaskSaveReqDTO, String.valueOf(reqVO.getProjectCode()), taskCode);
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
            }
            fillIncrementalExt(taskExt, incrementalConfig, flinkxJobJson, data,
                    prepareNodeCode, prepareNodeName, completeNodeCode, completeNodeName);
            CollectorEtlTaskExtService.updateById(taskExt);
        }

        //上线DS任务
        DsStatusRespDTO releaseResp = dsEtlTaskService.releaseTask("ONLINE", String.valueOf(reqVO.getProjectCode()), taskCode);
        String responseMsg = releaseResp == null ? null : releaseResp.getMsg();
        if (responseMsg != null && responseMsg.contains("SubWorkflowDefinition") && responseMsg.contains("is not online")) {
            throw new RuntimeException("存在未上线的子工作流，请先将所有子工作流上线");
        }
        if (releaseResp == null || !Boolean.TRUE.equals(releaseResp.getSuccess()) || !Boolean.TRUE.equals(releaseResp.getData())) {
            throw new ServiceException("上线任务失败！");
        }

        //更新任务状态为已上线
        updateTaskStatus(taskDO.getId(), "1");
        taskDO.setStatus("1");

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
            taskDO.setCode(taskCode); //确保使用DS编码
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
                    DsStatusRespDTO onlineScheduler = iDsEtlSchedulerService.onlineScheduler(taskDO.getProjectCode(), schedulerSaveReqVO.getDsId());
                    if (onlineScheduler == null || !Boolean.TRUE.equals(onlineScheduler.getSuccess()) || !Boolean.TRUE.equals(onlineScheduler.getData())) {
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
                DsStatusRespDTO offlineScheduler = iDsEtlSchedulerService.offlineScheduler(taskDO.getProjectCode(), schedulerDO.getDsId());
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

        CollectorEtlTaskExtDO taskExt = CollectorEtlTaskExtService.getByTaskId(taskDO.getId());
        String dsCode;
        if (taskExt != null && StringUtils.isNotEmpty(taskExt.getEtlTaskCode())) {
            dsCode = taskExt.getEtlTaskCode();
        } else {
            dsCode = taskDO.getCode();
        }

        stopRunningProcessInstances(taskDO.getId(), String.valueOf(taskDO.getProjectCode()), dsCode);
        collectorEtlIncrementalService.forceReleaseIncrementalTask(taskDO.getId());

        //下线DS任务
        DsStatusRespDTO dsStatusRespDTO = dsEtlTaskService.releaseTask("OFFLINE", String.valueOf(taskDO.getProjectCode()), dsCode);
        boolean processDefinitionMissing = isProcessDefinitionMissing(dsStatusRespDTO);
        if (!processDefinitionMissing && (dsStatusRespDTO == null || !dsStatusRespDTO.getSuccess())) {
            throw new ServiceException("卸载任务失败！");
        }

        //下线调度器
        CollectorEtlSchedulerDO schedulerDO = getCollectorEtlScheduler(taskDO.getCode(), taskDO.getId());
        try {
            if (schedulerDO != null && schedulerDO.getDsId() != null && schedulerDO.getDsId() > 0) {
                DsStatusRespDTO offlineScheduler = iDsEtlSchedulerService.offlineScheduler(taskDO.getProjectCode(), schedulerDO.getDsId());
                if (offlineScheduler == null || !Boolean.TRUE.equals(offlineScheduler.getData())) {
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
        if (processDefinitionMissing && taskExt != null) {
            CollectorEtlTaskExtService.removeById(taskExt.getId());
        }

        //更新任务状态为已下线
        updateTaskStatus(taskDO.getId(), "0");
    }

    private void stopRunningProcessInstances(Long taskId, String projectCode, String dsCode) {
        Set<Long> processInstanceIds = new LinkedHashSet<>();
        Long runningTaskInstanceId = CollectorEtlTaskInstanceService.getRunTaskInstance(taskId);
        if (runningTaskInstanceId != null) {
            processInstanceIds.add(runningTaskInstanceId);
        }

        try {
            List<com.datamaster.api.ds.api.etl.ds.ProcessInstance> dsInstances =
                    dsEtlExecutorService.listProcessInstances(projectCode, dsCode);
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
            log.warn("卸载任务时查询DS运行中流程实例异常，taskId={}，projectCode={}，taskCode={}，原因：{}",
                    taskId, projectCode, dsCode, e.getMessage(), e);
        }

        for (Long processInstanceId : processInstanceIds) {
            try {
                DsStatusRespDTO stopResult = dsEtlExecutorService.execute(DSExecuteDTO.builder()
                        .processInstanceId(processInstanceId)
                        .executeType(ExecuteType.STOP)
                        .build(), projectCode);
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

    private DsTaskSaveRespDTO updateDsTaskAllowingOnline(DsTaskSaveReqDTO request, String projectCode, String taskCode) {
        DsTaskSaveRespDTO response = dsEtlTaskService.updateTask(request, projectCode, taskCode);
        if (response == null || !StringUtils.containsIgnoreCase(response.getMsg(), "does not allow edit")) {
            return response;
        }
        DsStatusRespDTO offlineResp = dsEtlTaskService.releaseTask("OFFLINE", projectCode, taskCode);
        if (offlineResp == null || !Boolean.TRUE.equals(offlineResp.getSuccess())) {
            throw new ServiceException("更新DS任务前下线失败:" + (offlineResp == null ? "无响应" : offlineResp.getMsg()));
        }
        return dsEtlTaskService.updateTask(request, projectCode, taskCode);
    }

    private Long resolveTaskId(CollectorEtlNewNodeSaveReqVO reqVO) {
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
        if (reqVO.getProjectCode() != null) {
            filled.setProjectCode(reqVO.getProjectCode());
        }
        if (reqVO.getProjectId() != null) {
            filled.setProjectId(reqVO.getProjectId());
        }
        normalizeProjectCode(filled);
        return filled;
    }

    private void normalizeProjectCode(CollectorEtlNewNodeSaveReqVO reqVO) {
        if (reqVO.getProjectId() == null) {
            return;
        }
        String projectCode = taxonomyProjectApi.getProjectCodeByProjectId(reqVO.getProjectId());
        if (StringUtils.isNotBlank(projectCode)) {
            reqVO.setProjectCode(JSONUtils.convertToLong(projectCode));
        }
    }


    @Override
    public CollectorEtlTaskUpdateQueryRespVO getupdateQueryFront(Long id) {

        CollectorEtlTaskDO CollectorEtlTaskDO = CollectorEtlTaskMapper.selectById(id);
        CollectorEtlTaskUpdateQueryRespVO bean = new CollectorEtlTaskUpdateQueryRespVO(CollectorEtlTaskDO);

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
                , CollectorEtlTaskUpdateQueryRespVO.getProjectCode());

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
            DsNodeGenCodeRespDTO dsNodeGenCodeRespDTO = dsEtlNodeService.genCode(CollectorEtlNewNodeSaveReqVO.getProjectCode());
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
        taskInfo.put("projectCode", CollectorEtlNewNodeSaveReqVO.getProjectCode());
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

    private String nextDsNodeCode(Long projectCode) {
        DsNodeGenCodeRespDTO response = dsEtlNodeService.genCode(projectCode);
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
        String completeNodeCode = copiedCompleteNode == null
                ? nextDsNodeCode(Long.valueOf(task.getProjectCode())) : copiedCompleteNode.getCode();
        String completeNodeName = task.getName() + "-状态回写";

        DsTaskSaveReqDTO request = new DsTaskSaveReqDTO();
        request.setName(task.getName());
        request.setDescription(task.getDescription());
        request.setExecutionType(task.getExecutionType());
        request.setTaskDefinitionJson(TaskConverter.buildIncrementalFlinkxTaskDefinitionJson(
                copiedPrepareNode.getId(), task.getName() + "-增量边界准备", copiedPrepareNode.getCode(),
                copiedPrepareNode.getVersion(), copiedFlinkxNode.getId(), copiedFlinkxNode.getName(),
                copiedFlinkxNode.getCode(), copiedFlinkxNode.getVersion(),
                copiedCompleteNode == null ? null : copiedCompleteNode.getId(), completeNodeName,
                completeNodeCode, copiedCompleteNode == null ? 0 : copiedCompleteNode.getVersion(),
                incrementalCallbackUrl(incrementalPrepareUrl, task.getId()),
                incrementalCallbackUrl(incrementalCompleteUrl, task.getId()),
                task.getDraftJson(), getProjectWorkerGroup(task.getProjectCode())));
        ProcessTaskRelation prepareRelation = getDsTaskRelation(copiedDefinition, copiedPrepareNode.getCode());
        ProcessTaskRelation flinkxRelation = getDsTaskRelation(copiedDefinition, copiedFlinkxNode.getCode());
        ProcessTaskRelation completeRelation = copiedCompleteNode == null
                ? null : getDsTaskRelation(copiedDefinition, copiedCompleteNode.getCode());
        request.setTaskRelationJson(TaskConverter.buildIncrementalFlinkxTaskRelationJson(
                prepareRelation.getId(), flinkxRelation.getId(),
                completeRelation == null ? null : completeRelation.getId(),
                copiedPrepareNode.getCode(), copiedPrepareNode.getVersion(),
                copiedFlinkxNode.getCode(), copiedFlinkxNode.getVersion(),
                completeNodeCode, copiedCompleteNode == null ? 0 : copiedCompleteNode.getVersion()));
        List<Map<String, Object>> locations = JSONUtils.convertTaskDefinitionJson(task.getLocations());
        request.setLocations(TaskConverter.buildIncrementalFlinkxTaskLocationsJson(
                locations, copiedPrepareNode.getCode(), copiedFlinkxNode.getCode(), completeNodeCode));

        DsTaskSaveRespDTO response = dsEtlTaskService.updateTask(request, task.getProjectCode(), copiedDefinition.getCode());
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
                .set(CollectorEtlTaskExtDO::getCompleteNodeId, null)
                .set(CollectorEtlTaskExtDO::getCompleteNodeName, null)
                .set(CollectorEtlTaskExtDO::getCompleteNodeCode, null)
                .set(CollectorEtlTaskExtDO::getCompleteNodeVersion, null)
                .set(CollectorEtlTaskExtDO::getCompleteRelationId, null)
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
        taskExt.setCompleteNodeId(null);
        taskExt.setCompleteNodeName(null);
        taskExt.setCompleteNodeCode(null);
        taskExt.setCompleteNodeVersion(null);
        taskExt.setCompleteRelationId(null);
    }

    private String incrementalCallbackUrl(String baseUrl, Long taskId) {
        return baseUrl + "/" + taskId + "?processInstanceId=${system.workflow.instance.id}";
    }

    private String getProjectWorkerGroup(Long projectCode) {
        return getProjectWorkerGroup(projectCode == null ? null : String.valueOf(projectCode));
    }

    private String getProjectWorkerGroup(String projectCode) {
        if (StringUtils.isEmpty(projectCode)) {
            return "default";
        }
        return taxonomyProjectApi.getWorkerGroupByProjectCode(projectCode);
    }

}
