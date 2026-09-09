

package com.datamaster.metadata.service.qa.impl;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.datamaster.api.ds.api.base.DsStatusRespDTO;
import com.datamaster.api.ds.api.etl.*;
import com.datamaster.api.ds.api.etl.ds.ProcessDefinition;
import com.datamaster.api.ds.api.etl.ds.Schedule;
import com.datamaster.api.ds.api.etl.ds.TaskDefinition;
import com.datamaster.api.ds.api.service.etl.IDsEtlNodeService;
import com.datamaster.api.ds.api.service.etl.IDsEtlSchedulerService;
import com.datamaster.api.ds.api.service.etl.IDsEtlTaskService;
import com.datamaster.common.core.domain.AjaxResult;
import com.datamaster.common.core.domain.BaseEntity;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.httpClient.HeaderEntity;
import com.datamaster.common.httpClient.HttpUtils;
import com.datamaster.common.utils.JSONUtils;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.common.datasource.mgmt.api.dto.DatasourceRespDTO;
import com.datamaster.common.datasource.mgmt.api.IDatasourceApiService;
import com.datamaster.common.api.space.ISpaceApi;
import com.datamaster.metadata.api.qa.dto.QualitySummaryRespDTO;
import com.datamaster.metadata.api.service.qa.QualityTaskApiService;
import com.datamaster.metadata.controller.qa.vo.*;
import com.datamaster.metadata.dal.dataobject.qa.QualityLogDO;
import com.datamaster.metadata.dal.dataobject.qa.QualityTaskDO;
import com.datamaster.metadata.dal.dataobject.qa.QualityTaskEvaluateDO;
import com.datamaster.metadata.dal.dataobject.qa.QualityTaskObjDO;
import com.datamaster.metadata.dal.mapper.qa.QualityTaskMapper;
import com.datamaster.metadata.dal.mapper.qa.QualityLogMapper;
import com.datamaster.metadata.dal.mapper.qa.QualityTaskEvaluateMapper;
import com.datamaster.metadata.dal.mapper.qa.QualityTaskObjMapper;
import com.datamaster.metadata.service.qa.IEvaluateLogService;
import com.datamaster.metadata.service.qa.IQualityLogService;
import com.datamaster.metadata.service.qa.IQualityTaskEvaluateService;
import com.datamaster.metadata.service.qa.IQualityTaskObjService;
import com.datamaster.metadata.service.qa.IQualityTaskService;
import com.datamaster.metadata.utils.TaskConverter;
import com.datamaster.metadata.utils.model.TaskSaveReqInput;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

import javax.annotation.Resource;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.datamaster.common.core.domain.AjaxResult.error;
import static com.datamaster.common.core.domain.AjaxResult.success;

/**
 * 质量探查任务Service业务层处理
 *
 * @author Chaos
 * @date 2025-07-21
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class QualityTaskServiceImpl  extends ServiceImpl<QualityTaskMapper,QualityTaskDO> implements IQualityTaskService, QualityTaskApiService {

    @Value("${path.quality_url}")
    private String url;

    @Resource
    private ISpaceApi taxonomySpaceApi;

    @Resource
    private QualityTaskMapper QualityTaskMapper;
    @Resource
    private QualityTaskObjMapper QualityTaskObjMapper;
    @Resource
    private QualityTaskEvaluateMapper QualityTaskEvaluateMapper;
    @Resource
    private QualityLogMapper QualityLogMapper;

    @Resource
    private IQualityTaskEvaluateService QualityTaskEvaluateService;
    @Resource
    private IQualityTaskObjService QualityTaskObjService;
    @Resource
    private IDatasourceApiService daDatasourceApiService;
    @Resource
    private IDsEtlTaskService dsEtlTaskService;


    @Resource
    private IDsEtlSchedulerService iDsEtlSchedulerService;


    @Resource
    private IDsEtlNodeService dsEtlNodeService;

    @Resource
    private IQualityLogService QualityLogService;

    @Resource
    private IEvaluateLogService EvaluateLogService;

    @Override
    public PageResult<QualityTaskDO> getQualityTaskPage(QualityTaskPageReqVO pageReqVO) {
        return QualityTaskMapper.selectPage(pageReqVO);
    }

    @Override
    public QualitySummaryRespDTO getLatestQualitySummary(Long datasourceId, String tableName) {
        if (datasourceId == null || StringUtils.isBlank(tableName)) {
            return null;
        }
        List<QualityTaskObjDO> objects = QualityTaskObjMapper.selectList(Wrappers.lambdaQuery(QualityTaskObjDO.class)
                .eq(QualityTaskObjDO::getDatasourceId, datasourceId)
                .eq(QualityTaskObjDO::getTableName, tableName)
                .orderByDesc(BaseEntity::getCreateTime));
        if (CollectionUtils.isEmpty(objects)) {
            objects = QualityTaskObjMapper.selectList(Wrappers.lambdaQuery(QualityTaskObjDO.class)
                    .eq(QualityTaskObjDO::getDatasourceId, datasourceId)
                    .apply("LOWER(TABLE_NAME) = LOWER({0})", tableName)
                    .orderByDesc(BaseEntity::getCreateTime));
        }
        if (CollectionUtils.isEmpty(objects)) {
            return null;
        }
        Set<Long> taskIds = objects.stream()
                .map(QualityTaskObjDO::getTaskId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (CollectionUtils.isEmpty(taskIds)) {
            return null;
        }
        QualityLogDO latestLog = QualityLogMapper.selectOne(Wrappers.lambdaQuery(QualityLogDO.class)
                .in(QualityLogDO::getQualityId, taskIds)
                .orderByDesc(QualityLogDO::getStartTime, QualityLogDO::getEndTime, QualityLogDO::getUpdateTime, QualityLogDO::getId)
                .last("limit 1"));
        Long taskId = latestLog == null ? taskIds.iterator().next() : latestLog.getQualityId();
        QualityTaskDO task = taskId == null ? null : QualityTaskMapper.selectById(taskId);
        QualitySummaryRespDTO summary = new QualitySummaryRespDTO();
        summary.setTaskId(taskId);
        if (task != null) {
            summary.setTaskName(task.getTaskName());
        }
        if (latestLog != null) {
            summary.setLogId(latestLog.getId());
            summary.setLogName(latestLog.getName());
            summary.setSuccessFlag(latestLog.getSuccessFlag());
            summary.setStartTime(latestLog.getStartTime());
            summary.setEndTime(latestLog.getEndTime());
            summary.setScore(latestLog.getScore());
            summary.setProblemData(latestLog.getProblemData());
        }
        List<Long> objectIds = objects.stream()
                .filter(obj -> taskId == null || Objects.equals(taskId, obj.getTaskId()))
                .map(QualityTaskObjDO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        List<QualityTaskEvaluateDO> evaluates = QualityTaskEvaluateMapper.selectList(Wrappers.lambdaQuery(QualityTaskEvaluateDO.class)
                .eq(taskId != null, QualityTaskEvaluateDO::getTaskId, taskId)
                .in(CollectionUtils.isNotEmpty(objectIds), QualityTaskEvaluateDO::getObjId, objectIds)
                .orderByDesc(BaseEntity::getCreateTime)
                .last("limit 8"));
        if (CollectionUtils.isNotEmpty(evaluates)) {
            for (QualityTaskEvaluateDO evaluate : evaluates) {
                QualitySummaryRespDTO.Rule rule = new QualitySummaryRespDTO.Rule();
                rule.setId(evaluate.getId());
                rule.setName(evaluate.getName());
                rule.setRuleName(evaluate.getRuleName());
                rule.setRuleType(evaluate.getRuleType());
                rule.setDimensionType(evaluate.getDimensionType());
                rule.setEvaColumn(evaluate.getEvaColumn());
                rule.setWarningLevel(evaluate.getWarningLevel());
                rule.setStatus(evaluate.getStatus());
                rule.setErrDescription(evaluate.getErrDescription());
                rule.setSuggestion(evaluate.getSuggestion());
                summary.getRules().add(rule);
            }
        }
        return summary;
    }

    @Override
    public List<QualityLogDO> getQualityLogListByTable(Long datasourceId, String tableName) {
        if (datasourceId == null || StringUtils.isBlank(tableName)) {
            return new ArrayList<>();
        }
        // 1. 按数据源+表名查询质量探查任务对象（先精确匹配，再大小写不敏感兜底）
        List<QualityTaskObjDO> objects = QualityTaskObjMapper.selectList(Wrappers.lambdaQuery(QualityTaskObjDO.class)
                .eq(QualityTaskObjDO::getDatasourceId, datasourceId)
                .eq(QualityTaskObjDO::getTableName, tableName)
                .orderByDesc(BaseEntity::getCreateTime));
        if (CollectionUtils.isEmpty(objects)) {
            objects = QualityTaskObjMapper.selectList(Wrappers.lambdaQuery(QualityTaskObjDO.class)
                    .eq(QualityTaskObjDO::getDatasourceId, datasourceId)
                    .apply("LOWER(TABLE_NAME) = LOWER({0})", tableName)
                    .orderByDesc(BaseEntity::getCreateTime));
        }
        if (CollectionUtils.isEmpty(objects)) {
            return new ArrayList<>();
        }
        // 2. 收集关联的质量任务ID
        Set<Long> taskIds = objects.stream()
                .map(QualityTaskObjDO::getTaskId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (CollectionUtils.isEmpty(taskIds)) {
            return new ArrayList<>();
        }
        // 3. 查询该表全部探查日志，按开始时间倒序（多次探查结果）
        return QualityLogMapper.selectList(Wrappers.lambdaQuery(QualityLogDO.class)
                .in(QualityLogDO::getQualityId, taskIds)
                .orderByDesc(QualityLogDO::getStartTime, QualityLogDO::getEndTime, QualityLogDO::getUpdateTime, QualityLogDO::getId));
    }

    @Override
    public Long createQualityTask(QualityTaskSaveReqVO createReqVO) {
        String assetFlag = createReqVO.getAssetFlag();
        if(StringUtils.equals("1",assetFlag)){
            MPJLambdaWrapper<QualityTaskDO> wrapper = new MPJLambdaWrapper<>();
            wrapper.selectAll(QualityTaskDO.class)
                    .eq(QualityTaskDO::getAssetFlag,"1")
                    .eq(QualityTaskDO::getAssetId,createReqVO.getAssetId());
            List<QualityTaskDO> taskDO = QualityTaskMapper.selectList(wrapper);
            if(CollectionUtils.isNotEmpty(taskDO)){
                return taskDO.get(0).getId();
            }
        }

        QualityTaskDO dictType = BeanUtils.toBean(createReqVO, QualityTaskDO.class);
        fillSpaceRelation(dictType);
        QualityTaskMapper.insert(dictType);
        List<QualityTaskObjSaveReqVO> QualityTaskObjSaveReqVO = createReqVO.getQualityTaskObjSaveReqVO();
        for (QualityTaskObjSaveReqVO qualityTaskObjSaveReqVO : QualityTaskObjSaveReqVO) {
            qualityTaskObjSaveReqVO.setTaskId(dictType.getId());
            Long QualityTaskObj = QualityTaskObjService.createQualityTaskObj(qualityTaskObjSaveReqVO);
            qualityTaskObjSaveReqVO.setId(QualityTaskObj);
        }
        Map<String, QualityTaskObjSaveReqVO> collect = QualityTaskObjSaveReqVO.stream().collect(Collectors.toMap(s -> s.getDatasourceId() + s.getTableName(), Function.identity()));
        List<QualityTaskEvaluateSaveReqVO> QualityTaskEvaluateSaveReqVO = createReqVO.getQualityTaskEvaluateSaveReqVO();
        if (QualityTaskEvaluateSaveReqVO != null) {
            for (QualityTaskEvaluateSaveReqVO qualityTaskEvaluateSaveReqVO : QualityTaskEvaluateSaveReqVO) {
                QualityTaskObjSaveReqVO QualityTaskObjSaveReqVO1 = collect.get(qualityTaskEvaluateSaveReqVO.getDatasourceId() + qualityTaskEvaluateSaveReqVO.getTableName());
                if (QualityTaskObjSaveReqVO1 != null) {
                    qualityTaskEvaluateSaveReqVO.setTaskId(dictType.getId());
                    qualityTaskEvaluateSaveReqVO.setObjId(QualityTaskObjSaveReqVO1.getId());
                    qualityTaskEvaluateSaveReqVO.setObjName(QualityTaskObjSaveReqVO1.getName());
                    handleCharacterValidationRule(qualityTaskEvaluateSaveReqVO);
                    QualityTaskEvaluateService.createQualityTaskEvaluate(qualityTaskEvaluateSaveReqVO);
                }
            }
        }

        return dictType.getId();
    }

    @Override
    public int updateQualityTask(QualityTaskSaveReqVO updateReqVO) {
        // 相关校验
        QualityTaskDO dictType = BeanUtils.toBean(updateReqVO, QualityTaskDO.class);
        fillSpaceRelation(dictType);
        List<QualityTaskObjSaveReqVO> QualityTaskObjSaveReqVO = updateReqVO.getQualityTaskObjSaveReqVO();
        for (QualityTaskObjSaveReqVO qualityTaskObjSaveReqVO : QualityTaskObjSaveReqVO) {
            qualityTaskObjSaveReqVO.setTaskId(dictType.getId());
            if (qualityTaskObjSaveReqVO.getId() != null) {
                QualityTaskObjService.updateQualityTaskObj(qualityTaskObjSaveReqVO);
            } else {
                Long QualityTaskObj = QualityTaskObjService.createQualityTaskObj(qualityTaskObjSaveReqVO);
                qualityTaskObjSaveReqVO.setId(QualityTaskObj);
            }
        }
        Map<String, QualityTaskObjSaveReqVO> collect = QualityTaskObjSaveReqVO.stream().collect(Collectors.toMap(s -> s.getDatasourceId() + s.getTableName(), Function.identity()));
        List<QualityTaskEvaluateSaveReqVO> QualityTaskEvaluateSaveReqVO = updateReqVO.getQualityTaskEvaluateSaveReqVO();
        if (QualityTaskEvaluateSaveReqVO != null) {
            for (QualityTaskEvaluateSaveReqVO qualityTaskEvaluateSaveReqVO : QualityTaskEvaluateSaveReqVO) {
                QualityTaskObjSaveReqVO QualityTaskObjSaveReqVO1 = collect.get(qualityTaskEvaluateSaveReqVO.getDatasourceId() + qualityTaskEvaluateSaveReqVO.getTableName());
                if (QualityTaskObjSaveReqVO1 != null) {
                    qualityTaskEvaluateSaveReqVO.setObjId(QualityTaskObjSaveReqVO1.getId());
                    qualityTaskEvaluateSaveReqVO.setObjName(QualityTaskObjSaveReqVO1.getName());
                }
                handleCharacterValidationRule(qualityTaskEvaluateSaveReqVO);
                if (qualityTaskEvaluateSaveReqVO.getId() != null) {
                    QualityTaskEvaluateService.updateQualityTaskEvaluate(qualityTaskEvaluateSaveReqVO);
                } else {
                    qualityTaskEvaluateSaveReqVO.setTaskId(dictType.getId());
                    QualityTaskEvaluateService.createQualityTaskEvaluate(qualityTaskEvaluateSaveReqVO);
                }
            }
        }
        return QualityTaskMapper.updateById(dictType);
    }
    @Override
    public int removeQualityTask(Collection<Long> idList) {
        // 批量删除质量探查任务
        for (Long id : idList) {
            // 查询 DaDiscoveryTaskDO 详情
            QualityTaskDO QualityTaskDO = QualityTaskMapper.selectById(id);
            if (QualityTaskDO != null &&
                    (QualityTaskDO.getSystemJobId() != null || !StringUtils.equals("0",QualityTaskDO.getTaskCode())) ) {
                // 提取 systemJobId
                if(StringUtils.equals("0",QualityTaskDO.getStatus())){
                    throw new ServiceException("上线任务，不允删除，请先下线！");
                }
                String spaceCode = resolveSpaceCode(QualityTaskDO);
                DsStatusRespDTO dsStatusRespDTO = dsEtlTaskService.deleteTask(spaceCode, QualityTaskDO.getTaskCode());
            }
        }
        return QualityTaskMapper.deleteBatchIds(idList);
    }

    @Override
    public QualityTaskRespVO getQualityTaskAsset(QualityTaskAssetReqVO QualityTaskAssetReqVO) {
        MPJLambdaWrapper<QualityTaskDO> wrapper = new MPJLambdaWrapper<>();
        wrapper.selectAll(QualityTaskDO.class)
                .eq(QualityTaskDO::getAssetFlag,"1")
                .eq(QualityTaskDO::getAssetId,QualityTaskAssetReqVO.getAssetId());
        QualityTaskDO taskDO = QualityTaskMapper.selectOne(wrapper);
        if(taskDO == null){
            return null;
        }
        QualityTaskRespVO QualityTaskRespVO = buildQualityTaskDetail(taskDO);
        QualityTaskAssetReqVO.setId(taskDO.getId());
        QualityLogDO log = QualityLogService.getQualityLogById(QualityTaskAssetReqVO);
        if(log == null){
            // 设置评分与问题数
            QualityTaskRespVO.setScore(0L);
            QualityTaskRespVO.setProblemData(0L);
            QualityTaskRespVO.setLogId(null);
            QualityTaskRespVO.setLastExecuteTime(null);
            return QualityTaskRespVO;
        }

        Map<String, Object> map = EvaluateLogService.sumTotalAndProblemTotalByTaskLogId(String.valueOf(log.getId()));

        // 获取总数与问题数（确保 null 安全）
        Long total = map.get("total") == null ? 0L : (Long) map.get("total");
        Long problemTotal = map.get("problemTotal") == null ? 0L : (Long) map.get("problemTotal");

        // 计算质量评分（百分比，保留两位小数）
        BigDecimal score = BigDecimal.ZERO;
        if (total > 0) {
            score = BigDecimal.valueOf(total - problemTotal)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
        }

        // 设置评分与问题数
        QualityTaskRespVO.setScore(score.longValue());
        QualityTaskRespVO.setProblemData(problemTotal);
        QualityTaskRespVO.setLogId(log.getId());
        QualityTaskRespVO.setLastExecuteTime(log.getStartTime());
        return QualityTaskRespVO;
    }

    @Override
    public QualityTaskRespVO getQualityTaskById(Long id) {
        QualityTaskDO taskDO = QualityTaskMapper.selectById(id);
        return taskDO != null ? buildQualityTaskDetail(taskDO) : null;
    }

    private QualityTaskRespVO buildQualityTaskDetail(QualityTaskDO QualityTaskDO) {
        QualityTaskRespVO bean = BeanUtils.toBean(QualityTaskDO, QualityTaskRespVO.class);

        // 数据对象列表
        LambdaQueryWrapperX<QualityTaskObjDO> objectLambdaQueryWrapperX = new LambdaQueryWrapperX<>();
        objectLambdaQueryWrapperX.eq(QualityTaskObjDO::getTaskId , QualityTaskDO.getId());
        List<QualityTaskObjDO> list = QualityTaskObjService.list(objectLambdaQueryWrapperX);

        List<QualityTaskObjRespVO> newList = new ArrayList<>();
        for (QualityTaskObjDO obj : list) {
            DatasourceRespDTO ds = daDatasourceApiService.getDatasourceById(obj.getDatasourceId());
            QualityTaskObjRespVO vo = BeanUtils.toBean(obj, QualityTaskObjRespVO.class);
            if (ds != null) {
                vo.setDatasourceType(ds.getDatasourceType());
                vo.setDatasourceConfig(ds.getDatasourceConfig());
            }
            newList.add(vo);
        }

        // 规则列表
        LambdaQueryWrapperX<QualityTaskEvaluateDO> evaWrapper = new LambdaQueryWrapperX<>();
        evaWrapper.eq(QualityTaskEvaluateDO::getTaskId , QualityTaskDO.getId());
        List<QualityTaskEvaluateDO> evaList = QualityTaskEvaluateService.list(evaWrapper);

        List<QualityTaskEvaluateRespVO> evaRespList = new ArrayList<>();
        for (QualityTaskEvaluateDO eva : evaList) {
            handleCharacterValidationRule(eva);
            evaRespList.add(BeanUtils.toBean(eva, QualityTaskEvaluateRespVO.class));
        }

        bean.setQualityTaskObjSaveReqVO(newList);
        bean.setQualityTaskEvaluateRespVOS(evaRespList);
        return bean;
    }

    @Override
    public List<QualityTaskDO> getQualityTaskList() {
        return QualityTaskMapper.selectList();
    }

    @Override
    public Map<Long, QualityTaskDO> getQualityTaskMap() {
        List<QualityTaskDO> QualityTaskList = QualityTaskMapper.selectList();
        return QualityTaskList.stream()
                .collect(Collectors.toMap(
                        QualityTaskDO::getId,
                        QualityTaskDO -> QualityTaskDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing
                ));
    }


        /**
         * 导入质量探查任务数据
         *
         * @param importExcelList 质量探查任务数据列表
         * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
         * @param operName 操作用户
         * @return 结果
         */
        @Override
        public String importQualityTask(List<QualityTaskRespVO> importExcelList, boolean isUpdateSupport, String operName) {
            if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
                throw new ServiceException("导入数据不能为空！");
            }

            int successNum = 0;
            int failureNum = 0;
            List<String> successMessages = new ArrayList<>();
            List<String> failureMessages = new ArrayList<>();

            for (QualityTaskRespVO respVO : importExcelList) {
                try {
                    QualityTaskDO QualityTaskDO = BeanUtils.toBean(respVO, QualityTaskDO.class);
                    Long QualityTaskId = respVO.getId();
                    if (isUpdateSupport) {
                        if (QualityTaskId != null) {
                            QualityTaskDO existingQualityTask = QualityTaskMapper.selectById(QualityTaskId);
                            if (existingQualityTask != null) {
                                QualityTaskMapper.updateById(QualityTaskDO);
                                successNum++;
                                successMessages.add("数据更新成功，ID为 " + QualityTaskId + " 的质量探查任务记录。");
                            } else {
                                failureNum++;
                                failureMessages.add("数据更新失败，ID为 " + QualityTaskId + " 的质量探查任务记录不存在。");
                            }
                        } else {
                            failureNum++;
                            failureMessages.add("数据更新失败，某条记录的ID不存在。");
                        }
                    } else {
                        QueryWrapper<QualityTaskDO> queryWrapper = new QueryWrapper<>();
                        queryWrapper.eq("id", QualityTaskId);
                        QualityTaskDO existingQualityTask = QualityTaskMapper.selectOne(queryWrapper);
                        if (existingQualityTask == null) {
                            QualityTaskMapper.insert(QualityTaskDO);
                            successNum++;
                            successMessages.add("数据插入成功，ID为 " + QualityTaskId + " 的质量探查任务记录。");
                        } else {
                            failureNum++;
                            failureMessages.add("数据插入失败，ID为 " + QualityTaskId + " 的质量探查任务记录已存在。");
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
    public String verifyInterfaceValue(QualityTaskEvaluateSaveReqVO QualityTaskEvaluate) {
        // 处理正则
//        JSONObject jsonObject = JSONObject.parseObject(QualityTaskEvaluate.getRule());
//        List<String> lists = jsonObject.getList("allowedChars", String.class);
//        String s = this.validateInputWithRegex(lists);

        Map<String, Object> map = this.buildRuleParamMap(QualityTaskEvaluate);
        map.put("dataId", QualityTaskEvaluate.getDatasourceId());
        map.put("inputValue", QualityTaskEvaluate.getTitle());
        List<HeaderEntity> headers = new ArrayList<>();
        HeaderEntity headerEntity = new HeaderEntity();
        headerEntity.setKey("Content-Type");
        headerEntity.setValue("application/json");
        headers.add(headerEntity);  // 设置请求头
        try {
            HttpUtils.ResponseObject responseObject = HttpUtils.sendPost(url + "/generateDataCheck", map, headers);
            System.out.println(responseObject.toString());
            // 强转并解析为 JSONObject
            JSONObject json = JSONObject.parseObject(String.valueOf(responseObject.getBody()));
            // 提取 data
            String data = json.getString("data");
            if (StringUtils.equals("1",data)) {
                return QualityTaskEvaluate.getTitle() + "，数据监测成功";
            }
            return QualityTaskEvaluate.getTitle() + "，不符合规则";
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public AjaxResult startQualityTask(Long id) {
        QualityTaskDO QualityTaskDO = QualityTaskMapper.selectById(id);
        if(QualityTaskDO == null){
            return error("任务不存在，请刷新后重试！");
        }
        if (!StringUtils.equals("0",QualityTaskDO.getStatus())){
            return error("任务状态错误，请刷新后重试！");
        }

        String spaceCode = resolveSpaceCode(QualityTaskDO);
        DsStartTaskReqDTO dsStartTaskReqDTO = TaskConverter.createDsStartTaskReqDTO(QualityTaskDO.getTaskCode(), QualityTaskDO.getWorkerGroup());

        DsStatusRespDTO dsStatusRespDTO = dsEtlTaskService.startTask(dsStartTaskReqDTO, spaceCode);

        return Boolean.TRUE.equals(dsStatusRespDTO == null ? null : dsStatusRespDTO.getSuccess())
                ? success() : error(dsStatusRespDTO == null ? "DolphinScheduler无响应" : dsStatusRespDTO.getMsg());
    }

    @Override
    public boolean updateQualityTaskStatus(QualityTaskSaveReqVO daDiscoveryTask) {
        QualityTaskRespVO QualityTaskById = this.getQualityTaskById(daDiscoveryTask.getId());
        String daDiscoveryTaskStatus = daDiscoveryTask.getStatus();

        validateTaskStatus(QualityTaskById, daDiscoveryTaskStatus);

        String spaceCode = resolveSpaceCode(
                QualityTaskById.getSpaceId(),
                QualityTaskById.getSpaceCode(),
                QualityTaskById.getCatCode()
        );
        daDiscoveryTask.setCycle(QualityTaskById.getCycle());
        Long systemJobId = QualityTaskById.getSystemJobId();
        if (StringUtils.equals(daDiscoveryTaskStatus, QualityTaskById.getStatus())) {

            return false;
        }
        if (StringUtils.equals("1", daDiscoveryTaskStatus)) {
            handleOfflineTask(spaceCode, QualityTaskById, systemJobId, daDiscoveryTask);
            return true;
        }

        handleOnlineTask(spaceCode, QualityTaskById, systemJobId, daDiscoveryTask);

        updateTaskStatusAndScheduler(spaceCode, daDiscoveryTask, systemJobId);

        return true;
    }



    @Override
    public JSONObject validationErrorDataSql(QualityTaskEvaluateSaveReqVO QualityTaskEvaluate) {
        Map<String, Object> objectObjectHashMap =  this.buildRuleParamMap(QualityTaskEvaluate);
        List<HeaderEntity> headers = new ArrayList<>();
        HeaderEntity headerEntity = new HeaderEntity();
        headerEntity.setKey("Content-Type");
        headerEntity.setValue("application/json");
        headers.add(headerEntity);  // 设置请求头
        try {
            HttpUtils.ResponseObject responseObject = HttpUtils.sendPost(url + "/generateValidationErrorDataSql", objectObjectHashMap, headers);
            System.out.println(responseObject.toString());
            // 强转并解析为 JSONObject
            JSONObject json = JSONObject.parseObject(String.valueOf(responseObject.getBody()));
            // 提取 data
            JSONObject data = json.getJSONObject("data");
            return data;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public JSONObject validationValidDataSql(QualityTaskEvaluateSaveReqVO QualityTaskEvaluate) {
        Map<String, Object> objectObjectHashMap =  this.buildRuleParamMap(QualityTaskEvaluate);
        List<HeaderEntity> headers = new ArrayList<>();
        HeaderEntity headerEntity = new HeaderEntity();
        headerEntity.setKey("Content-Type");
        headerEntity.setValue("application/json");
        headers.add(headerEntity);  // 设置请求头
        try {
            HttpUtils.ResponseObject responseObject = HttpUtils.sendPost(url + "/generateValidationValidDataSql", objectObjectHashMap, headers);
            System.out.println(responseObject.toString());
            // 强转并解析为 JSONObject
            JSONObject json = JSONObject.parseObject(String.valueOf(responseObject.getBody()));
            // 提取 data
            JSONObject data = json.getJSONObject("data");
            return data;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean updateDaDiscoveryTaskCronExpression(QualityTaskSaveReqVO daDiscoveryTask) {
        QualityTaskRespVO QualityTaskById = this.getQualityTaskById(daDiscoveryTask.getId());
        QualityTaskDO taskDO = QualityTaskMapper.selectById(daDiscoveryTask.getId());
        String spaceCode = resolveSpaceCode(taskDO);
        Long systemJobId = QualityTaskById.getSystemJobId();
        if(systemJobId != null){
            try {
                //     * 创建调度器 (只有任务发布了才能调用该接口)
                DsSchedulerUpdateReqDTO schedulerUpdateRequest = TaskConverter.createSchedulerUpdateRequest(systemJobId, daDiscoveryTask.getCycle(), QualityTaskById.getTaskCode(), QualityTaskById.getWorkerGroup());
                DsSchedulerRespDTO dsSchedulerRespDTO = iDsEtlSchedulerService.updateScheduler(schedulerUpdateRequest, spaceCode);
                if(dsSchedulerRespDTO == null || !dsSchedulerRespDTO.getSuccess()){
                    daDiscoveryTask.setTaskId(QualityTaskById.getTaskId());
                    daDiscoveryTask.setTaskCode(String.valueOf(QualityTaskById.getTaskCode()));
                    daDiscoveryTask.setNodeId(QualityTaskById.getNodeId());
                    daDiscoveryTask.setNodeCode(String.valueOf(QualityTaskById.getNodeCode()));
                    createSchedulerIfNeeded(spaceCode, daDiscoveryTask);
                }else {
                    Schedule schedule = dsSchedulerRespDTO.getData();
                    daDiscoveryTask.setSystemJobId(schedule.getId());
                }
            } catch (Exception e){
                throw new ServiceException("调度周期修改失败，请联系系统管理员！");

            }
        }

        // 更新数据发现任务
        QualityTaskDO updateObj = BeanUtils.toBean(daDiscoveryTask, QualityTaskDO.class);
        QualityTaskMapper.updateById(updateObj);
        return true;
    }

    private void validateTaskStatus(QualityTaskRespVO daDiscoveryTaskById, String daDiscoveryTaskStatus) {
        if (daDiscoveryTaskById == null || daDiscoveryTaskStatus == null) {
            throw new ServiceException("任务模版错误，未查询到调度信息！");
        }
    }

    private void handleOfflineTask(String spaceCode, QualityTaskRespVO daDiscoveryTaskById, Long systemJobId, QualityTaskSaveReqVO daDiscoveryTask) {
        // 只有在 DS 中有任务定义时才下线
        if (StringUtils.isNotEmpty(daDiscoveryTaskById.getTaskCode())) {
            DsStatusRespDTO respDTO = dsEtlTaskService.releaseTask("OFFLINE", spaceCode, daDiscoveryTaskById.getTaskCode());
            if (!isDsStatusSuccess(respDTO)) {
                throw new ServiceException("发布或下线任务，失败！");
            }

            if (systemJobId != null && systemJobId > 0) {
                DsStatusRespDTO offlined = iDsEtlSchedulerService.offlineScheduler(spaceCode, systemJobId);
                if (!isDsStatusSuccess(offlined)) {
                    throw new ServiceException("下线调度器，失败！");
                }
            }
        }

        // 更新数据发现任务
        QualityTaskDO updateObj = BeanUtils.toBean(daDiscoveryTask, QualityTaskDO.class);
        QualityTaskMapper.updateById(updateObj);
    }

    private void handleOnlineTask(String spaceCode, QualityTaskRespVO daDiscoveryTaskById, Long systemJobId, QualityTaskSaveReqVO daDiscoveryTask) {
        if (systemJobId == null || systemJobId < 1) {
            createNewProcessDefinition(spaceCode, daDiscoveryTaskById, daDiscoveryTask);
        } else if (daDiscoveryTaskById.getId() != null) {
            updateExistingProcessDefinition(spaceCode, daDiscoveryTaskById, daDiscoveryTask);
        }
    }

    private void createNewProcessDefinition(String spaceCode, QualityTaskRespVO daDiscoveryTaskById, QualityTaskSaveReqVO daDiscoveryTask) {
        TaskSaveReqInput input = new TaskSaveReqInput();
        input.setName(daDiscoveryTaskById.getTaskName() + StringUtils.generateRandomString());
        input.addHttpParam("id", "PARAMETER", daDiscoveryTaskById.getId());
        input.setId(daDiscoveryTaskById.getId());
        input.setWorkerGroup(daDiscoveryTaskById.getWorkerGroup());
        ProcessDefinition definition = this.createProcessDefinition(spaceCode, input);
        TaskDefinition firstTaskDefinition = TaskConverter.getFirstTaskDefinition(definition);

        daDiscoveryTask.setTaskId(definition.getId());
        daDiscoveryTask.setTaskCode(String.valueOf(definition.getCode()));
        daDiscoveryTask.setNodeId(firstTaskDefinition.getId());
        daDiscoveryTask.setNodeCode(String.valueOf(firstTaskDefinition.getCode()));
    }

    private void updateExistingProcessDefinition(String spaceCode, QualityTaskRespVO daDiscoveryTaskById, QualityTaskSaveReqVO daDiscoveryTask) {
        TaskSaveReqInput input = new TaskSaveReqInput();
        input.setName(daDiscoveryTaskById.getTaskName() + StringUtils.generateRandomString());
        input.addHttpParam("id", "PARAMETER", daDiscoveryTaskById.getId());
        input.setId(daDiscoveryTaskById.getId());
        input.setWorkerGroup(daDiscoveryTaskById.getWorkerGroup());

        input.setTaskId(daDiscoveryTaskById.getTaskId());
        input.setTaskCode(String.valueOf(daDiscoveryTaskById.getTaskCode()));
        input.setNodeId(daDiscoveryTaskById.getNodeId());
        input.setNodeCode(String.valueOf(daDiscoveryTaskById.getNodeCode()));

        ProcessDefinition definition = this.updateProcessDefinition(spaceCode, input);
        TaskDefinition firstTaskDefinition = TaskConverter.getFirstTaskDefinition(definition);

        daDiscoveryTask.setTaskId(definition.getId());
        daDiscoveryTask.setTaskCode(String.valueOf(definition.getCode()));
        daDiscoveryTask.setNodeId(firstTaskDefinition.getId());
        daDiscoveryTask.setNodeCode(String.valueOf(firstTaskDefinition.getCode()));
    }


    private void updateTaskStatusAndScheduler(String spaceCode, QualityTaskSaveReqVO daDiscoveryTask, Long systemJobId) {
        DsStatusRespDTO dsStatusRespDTO = dsEtlTaskService.releaseTask("ONLINE", spaceCode, daDiscoveryTask.getTaskCode());
        if (!isDsStatusSuccess(dsStatusRespDTO)) {
            throw new ServiceException("发布或下线任务，失败！");
        }

        String cycle = daDiscoveryTask.getCycle();
        if (StringUtils.isNotEmpty(cycle)) {
            if (systemJobId != null && systemJobId > 0) {
                updateExistingScheduler(spaceCode, daDiscoveryTask, systemJobId);
            } else {
                createNewScheduler(spaceCode, daDiscoveryTask);
            }

            DsStatusRespDTO dsStatusRespDTO1 = iDsEtlSchedulerService.onlineScheduler(spaceCode, daDiscoveryTask.getSystemJobId());
            if (!isDsStatusSuccess(dsStatusRespDTO1)) {
                throw new ServiceException("上线调度器，失败！");
            }
        } else if (systemJobId != null && systemJobId > 0) {
            // 已有调度器但取消了周期，下线并删除调度器
            iDsEtlSchedulerService.offlineScheduler(spaceCode, systemJobId);
        }

        // 更新数据发现任务
        QualityTaskDO updateObj = BeanUtils.toBean(daDiscoveryTask, QualityTaskDO.class);
        QualityTaskMapper.updateById(updateObj);
    }


    private void updateExistingScheduler(String spaceCode, QualityTaskSaveReqVO daDiscoveryTask, Long systemJobId) {
        DsSchedulerUpdateReqDTO schedulerUpdateRequest = TaskConverter.createSchedulerUpdateRequest(systemJobId, daDiscoveryTask.getCycle(), daDiscoveryTask.getTaskCode(), daDiscoveryTask.getWorkerGroup());
        DsSchedulerRespDTO dsSchedulerRespDTO = iDsEtlSchedulerService.updateScheduler(schedulerUpdateRequest, spaceCode);
        if (dsSchedulerRespDTO == null || !dsSchedulerRespDTO.getSuccess()) {
            createSchedulerIfNeeded(spaceCode, daDiscoveryTask);
        } else {
            Schedule schedule = dsSchedulerRespDTO.getData();
            daDiscoveryTask.setSystemJobId(schedule.getId());
        }
    }

    private void createNewScheduler(String spaceCode, QualityTaskSaveReqVO daDiscoveryTask) {
        DsSchedulerSaveReqDTO dsSchedulerSaveReqDTO = TaskConverter.createSchedulerRequest(daDiscoveryTask.getCycle(), daDiscoveryTask.getTaskCode(), daDiscoveryTask.getWorkerGroup());
        DsSchedulerRespDTO dsSchedulerRespDTO = iDsEtlSchedulerService.saveScheduler(dsSchedulerSaveReqDTO, spaceCode);
        if (dsSchedulerRespDTO == null || !dsSchedulerRespDTO.getSuccess()) {
            createSchedulerIfNeeded(spaceCode, daDiscoveryTask);
        } else {
            Schedule schedule = dsSchedulerRespDTO.getData();
            daDiscoveryTask.setSystemJobId(schedule.getId());
        }
    }


    private void createSchedulerIfNeeded(String spaceCode, QualityTaskSaveReqVO daDiscoveryTask) {
        DsSchedulerRespDTO byTaskCode = iDsEtlSchedulerService.getByTaskCode(spaceCode, daDiscoveryTask.getTaskCode());
        if (byTaskCode == null || !byTaskCode.getSuccess()) {
            //     * 创建调度器 (只有任务发布了才能调用该接口)
            DsSchedulerSaveReqDTO dsSchedulerSaveReqDTO = TaskConverter.createSchedulerRequest(daDiscoveryTask.getCycle(),daDiscoveryTask.getTaskCode(), daDiscoveryTask.getWorkerGroup());
            DsSchedulerRespDTO saveScheduler = iDsEtlSchedulerService.saveScheduler(dsSchedulerSaveReqDTO, spaceCode);
            if(saveScheduler == null || !saveScheduler.getSuccess()){
                throw new ServiceException("创建调度器，失败！");
            }
            Schedule schedule = saveScheduler.getData();

            daDiscoveryTask.setSystemJobId(schedule.getId());
            return;
        }
        Schedule schedule = byTaskCode.getData();
        daDiscoveryTask.setSystemJobId(schedule.getId());
        DsSchedulerUpdateReqDTO schedulerUpdateRequest = TaskConverter.createSchedulerUpdateRequest(schedule.getId(), daDiscoveryTask.getCycle(), daDiscoveryTask.getTaskCode(), daDiscoveryTask.getWorkerGroup());
        DsSchedulerRespDTO updated = iDsEtlSchedulerService.updateScheduler(schedulerUpdateRequest, spaceCode);
        if (updated == null || !updated.getSuccess()) {
            throw new ServiceException("更新调度器，失败！");
        }
    }

    private boolean isDsStatusSuccess(DsStatusRespDTO response) {
        if (response == null || Boolean.FALSE.equals(response.getSuccess()) || Boolean.FALSE.equals(response.getData())) {
            return false;
        }
        return Boolean.TRUE.equals(response.getSuccess())
                || Boolean.TRUE.equals(response.getData())
                || StringUtils.equalsIgnoreCase(response.getMsg(), "success");
    }

    public ProcessDefinition updateProcessDefinition(String spaceCode, TaskSaveReqInput input) {
        if (StringUtils.isBlank(input.getNodeCode())) {
            Long nodeUniqueKey = this.getNodeUniqueKey(TaskConverter.stringToLong(spaceCode));
            input.setNodeCode(TaskConverter.longToString(nodeUniqueKey));
        }

        DsTaskSaveReqDTO dsTaskSaveReqDTO = TaskConverter.buildDsTaskSaveReq(input);
        DsTaskSaveRespDTO task = dsEtlTaskService.updateTask(dsTaskSaveReqDTO, spaceCode, input.getTaskCode());

        if (task == null || !Boolean.TRUE.equals(task.getSuccess())) {
            throw new ServiceException("任务状态修改失败：" + (task == null || task.getMsg() == null ? "请联系系统管理员" : task.getMsg()));
        }
        ProcessDefinition data = task.getData();
        return data; // 返回创建结果
    }

    public ProcessDefinition createProcessDefinition(String spaceCode, TaskSaveReqInput input) {
        Long nodeUniqueKey = this.getNodeUniqueKey(TaskConverter.stringToLong(spaceCode));

        input.setNodeCode(TaskConverter.longToString(nodeUniqueKey));

        DsTaskSaveReqDTO dsTaskSaveReqDTO = TaskConverter.buildDsTaskSaveReq(input);
        DsTaskSaveRespDTO task = dsEtlTaskService.createTask(dsTaskSaveReqDTO, TaskConverter.stringToLong(spaceCode));

        if (task == null || !Boolean.TRUE.equals(task.getSuccess())) {
            throw new ServiceException("任务状态修改失败，请联系系统管理员"); // 抛出任务定义创建错误的异常
        }
        ProcessDefinition data = task.getData();
        return data; // 返回创建结果
    }

    public Long getNodeUniqueKey(Long spaceCode) {
        try {
            DsNodeGenCodeRespDTO dsNodeGenCodeRespDTO = dsEtlNodeService.genCode(spaceCode);
            return dsNodeGenCodeRespDTO.getData().get(0);
        } catch (Exception e){
            throw new ServiceException("任务状态修改失败，请联系系统管理员"); // 抛出任务定义创建错误的异常
        }
    }

    private void fillSpaceRelation(QualityTaskDO taskDO) {
        if (taskDO == null) {
            return;
        }

        if (taskDO.getSpaceId() != null) {
            String spaceCode = taxonomySpaceApi.getSpaceCodeBySpaceId(taskDO.getSpaceId());
            if (StringUtils.isNotEmpty(spaceCode)) {
                taskDO.setSpaceCode(spaceCode);
            }
        } else if (taskDO.getSpaceId() == null && StringUtils.isNotEmpty(taskDO.getSpaceCode())) {
            taskDO.setSpaceId(taxonomySpaceApi.getSpaceIdBySpaceCode(taskDO.getSpaceCode()));
        }

        if ((taskDO.getSpaceId() == null || StringUtils.isEmpty(taskDO.getSpaceCode()))
                && StringUtils.isNotEmpty(taskDO.getCatCode())) {
            QualityTaskDO catSpace = QualityTaskMapper.selectQualityCatSpaceByCode(taskDO.getCatCode());
            if (catSpace != null) {
                if (taskDO.getSpaceId() == null) {
                    taskDO.setSpaceId(catSpace.getSpaceId());
                }
                if (StringUtils.isEmpty(taskDO.getSpaceCode())) {
                    taskDO.setSpaceCode(catSpace.getSpaceCode());
                }
            }
        }
    }

    private String resolveSpaceCode(QualityTaskDO taskDO) {
        if (taskDO == null) {
            throw new ServiceException("质量任务不存在");
        }
        fillSpaceRelation(taskDO);
        return resolveSpaceCode(taskDO.getSpaceId(), taskDO.getSpaceCode(), taskDO.getCatCode());
    }

    private String resolveSpaceCode(Long spaceId, String spaceCode, String catCode) {
        if (spaceId != null) {
            String code = taxonomySpaceApi.getSpaceCodeBySpaceId(spaceId);
            if (StringUtils.isNotEmpty(code)) {
                return code;
            }
        }
        if (StringUtils.isNotEmpty(catCode)) {
            QualityTaskDO catSpace = QualityTaskMapper.selectQualityCatSpaceByCode(catCode);
            if (catSpace != null) {
                if (catSpace.getSpaceId() != null) {
                    String code = taxonomySpaceApi.getSpaceCodeBySpaceId(catSpace.getSpaceId());
                    if (StringUtils.isNotEmpty(code)) {
                        return code;
                    }
                }
                if (StringUtils.isNotEmpty(catSpace.getSpaceCode())) {
                    return catSpace.getSpaceCode();
                }
            }
        }
        if (StringUtils.isNotEmpty(spaceCode)) {
            return spaceCode;
        }
        throw new ServiceException("质量任务未关联空间或关联的空间编码不存在");
    }

    /**
     * 拼接正则表达式
     * @param value
     * @return
     */
    public static String validateInputWithRegex(List<String> value) {
        Map<String, String> map = new HashMap<>();
        // 数字
        map.put("1", "0-9");
        // 字母
        map.put("2", "a-zA-Z");
        // 空格
        map.put("3", "\\s");
        // 特殊符号
//        map.put("4", "!@#$%^&*(),.?" +'"' +":{}|<>");
//        map.put("4", "!\"#$%&'()*+,\\-./:;<=>?@[\\\\]^_`{|}~");
//        map.put("4", "!\"#$%&'()*+,\\-./:;<=>?@\\[\\]\\^_`{|}~");
        map.put("4", "[:punct:]");
//        map.put("4", "\\p{P}\\p{S}");
        // !@#$%^&*(),.?":{}|<>
        String s1 = "";
        for (String s : value) {
            s1 += map.get(s);

        }
        s1 = "^[" + s1 + "]+$";
        return s1;
    }

    /**
     * @param QualityTaskEvaluate
     * @return
     */
    public static Map<String, Object> buildRuleParamMap(QualityTaskEvaluateSaveReqVO QualityTaskEvaluate) {
        Map<String, Object> paramMap = new HashMap<>();

        // 1. 数据源 ID
        paramMap.put("dataId", QualityTaskEvaluate.getDatasourceId());

        // 2. 表名
        paramMap.put("tableName", QualityTaskEvaluate.getTableName());

        // 3. 规则类型
        paramMap.put("ruleType", QualityTaskEvaluate.getRuleType());

        // 4. 分页信息（临时写死 ruleType，如后续有分页参数可调整）
        paramMap.put("pageNum", QualityTaskEvaluate.getPageNum());
        paramMap.put("pageSize", QualityTaskEvaluate.getPageSize());


        String stringObjectMap = buildCharacterValidationRule(QualityTaskEvaluate.getRule(), QualityTaskEvaluate.getRuleType());

        // 5. 规则配置
        paramMap.put("config",  JSONUtils.convertTaskDefinitionJsonMap(stringObjectMap));

        // 6. 评估字段
        paramMap.put("evaColumn", QualityTaskEvaluate.getEvaColumn());

        // 7. where 条件
        paramMap.put("whereClause", QualityTaskEvaluate.getWhereClause());

        return paramMap;
    }
    /**
     * 处理 CHARACTER_VALIDATION 规则
     * 兼容 SaveReqVO 与 DO 两种类型
     */
    public static void handleCharacterValidationRule(QualityTaskEvaluateSaveReqVO qualityTaskEvaluateSaveReqVO) {
        if (qualityTaskEvaluateSaveReqVO == null) {
            return;
        }
        String newRule = buildCharacterValidationRule(
                qualityTaskEvaluateSaveReqVO.getRule(),
                qualityTaskEvaluateSaveReqVO.getRuleType()
        );
        if (newRule != null) {
            qualityTaskEvaluateSaveReqVO.setRule(newRule);
        }
    }

    public static void handleCharacterValidationRule(QualityTaskEvaluateDO evaluateDO) {
        if (evaluateDO == null) {
            return;
        }
        String newRule = buildCharacterValidationRule(
                evaluateDO.getRule(),
                evaluateDO.getRuleType()
        );
        if (newRule != null) {
            evaluateDO.setRule(newRule);
        }
    }

    /**
     * 公共内部逻辑
     */
    private static String buildCharacterValidationRule(String ruleJson, String ruleType) {
        if (StringUtils.isBlank(ruleJson) || !"CHARACTER_VALIDATION".equals(ruleType)) {
            return ruleJson;
        }

        JSONObject jsonObject = JSONObject.parseObject(ruleJson);
        String useRegexFlag = MapUtils.getString(jsonObject, "useRegexFlag", "0");

        if (StringUtils.equals("0",useRegexFlag)) {
            List<String> lists = jsonObject.getJSONArray("allowedChars").toJavaList(String.class);
            String regex = validateInputWithRegex(lists);

            jsonObject.put("regex", regex);
            jsonObject.put("allowedCalue", regex);

            return jsonObject.toJSONString();
        }
        return jsonObject.toJSONString();
    }

    @Override
    public Long getCountByCatCode(String catCode) {
        return baseMapper.selectCount(Wrappers.lambdaQuery(QualityTaskDO.class)
                .likeRight(QualityTaskDO::getCatCode, catCode));
    }

}
