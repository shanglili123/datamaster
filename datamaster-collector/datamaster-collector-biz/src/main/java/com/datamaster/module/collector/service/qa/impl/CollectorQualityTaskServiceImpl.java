

package com.datamaster.module.collector.service.qa.impl;

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
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.httpClient.HeaderEntity;
import com.datamaster.common.httpClient.HttpUtils;
import com.datamaster.common.utils.JSONUtils;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.module.assets.api.datasource.dto.AssetsDatasourceRespDTO;
import com.datamaster.module.assets.api.service.asset.IAssetsDatasourceApiService;
import com.datamaster.module.collector.api.service.qa.CollectorQualityTaskApiService;
import com.datamaster.module.collector.controller.admin.qa.vo.*;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorQualityLogDO;
import com.datamaster.module.collector.dal.dataobject.qa.CollectorQualityTaskDO;
import com.datamaster.module.collector.dal.dataobject.qa.CollectorQualityTaskEvaluateDO;
import com.datamaster.module.collector.dal.dataobject.qa.CollectorQualityTaskObjDO;
import com.datamaster.module.collector.dal.mapper.qa.CollectorQualityTaskMapper;
import com.datamaster.module.collector.service.etl.ICollectorEvaluateLogService;
import com.datamaster.module.collector.service.etl.ICollectorQualityLogService;
import com.datamaster.module.collector.service.qa.ICollectorQualityTaskEvaluateService;
import com.datamaster.module.collector.service.qa.ICollectorQualityTaskObjService;
import com.datamaster.module.collector.service.qa.ICollectorQualityTaskService;
import com.datamaster.module.collector.utils.CollectorTaskConverter;
import com.datamaster.module.collector.utils.model.TaskSaveReqInput;
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
 * 数据质量任务Service业务层处理
 *
 * @author Chaos
 * @date 2025-07-21
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class CollectorQualityTaskServiceImpl  extends ServiceImpl<CollectorQualityTaskMapper,CollectorQualityTaskDO> implements ICollectorQualityTaskService, CollectorQualityTaskApiService {

    private static String projectCode;

    @Value("${path.quality_url}")
    private String url;

    @Value("${ds.http_quality_projectCode}")
    private void setDefaultProjectCode(String projectCode) {
        this.projectCode = projectCode;
    }
    @Resource
    private CollectorQualityTaskMapper CollectorQualityTaskMapper;

    @Resource
    private ICollectorQualityTaskEvaluateService CollectorQualityTaskEvaluateService;
    @Resource
    private ICollectorQualityTaskObjService CollectorQualityTaskObjService;
    @Resource
    private IAssetsDatasourceApiService daDatasourceApiService;
    @Resource
    private IDsEtlTaskService dsEtlTaskService;


    @Resource
    private IDsEtlSchedulerService iDsEtlSchedulerService;


    @Resource
    private IDsEtlNodeService dsEtlNodeService;

    @Resource
    private ICollectorQualityLogService CollectorQualityLogService;

    @Resource
    private ICollectorEvaluateLogService CollectorEvaluateLogService;

    @Override
    public PageResult<CollectorQualityTaskDO> getCollectorQualityTaskPage(CollectorQualityTaskPageReqVO pageReqVO) {
        return CollectorQualityTaskMapper.selectPage(pageReqVO);
    }

    @Override
    public Long createCollectorQualityTask(CollectorQualityTaskSaveReqVO createReqVO) {
        String assetFlag = createReqVO.getAssetFlag();
        if(StringUtils.equals("1",assetFlag)){
            MPJLambdaWrapper<CollectorQualityTaskDO> wrapper = new MPJLambdaWrapper<>();
            wrapper.selectAll(CollectorQualityTaskDO.class)
                    .eq(CollectorQualityTaskDO::getAssetFlag,"1")
                    .eq(CollectorQualityTaskDO::getAssetId,createReqVO.getAssetId());
            List<CollectorQualityTaskDO> taskDO = CollectorQualityTaskMapper.selectList(wrapper);
            if(CollectionUtils.isNotEmpty(taskDO)){
                return taskDO.get(0).getId();
            }
        }

        CollectorQualityTaskDO dictType = BeanUtils.toBean(createReqVO, CollectorQualityTaskDO.class);
        CollectorQualityTaskMapper.insert(dictType);
        List<CollectorQualityTaskObjSaveReqVO> CollectorQualityTaskObjSaveReqVO = createReqVO.getCollectorQualityTaskObjSaveReqVO();
        for (CollectorQualityTaskObjSaveReqVO qualityTaskObjSaveReqVO : CollectorQualityTaskObjSaveReqVO) {
            qualityTaskObjSaveReqVO.setTaskId(dictType.getId());
            Long CollectorQualityTaskObj = CollectorQualityTaskObjService.createCollectorQualityTaskObj(qualityTaskObjSaveReqVO);
            qualityTaskObjSaveReqVO.setId(CollectorQualityTaskObj);
        }
        Map<String, CollectorQualityTaskObjSaveReqVO> collect = CollectorQualityTaskObjSaveReqVO.stream().collect(Collectors.toMap(s -> s.getDatasourceId() + s.getTableName(), Function.identity()));
        List<CollectorQualityTaskEvaluateSaveReqVO> CollectorQualityTaskEvaluateSaveReqVO = createReqVO.getCollectorQualityTaskEvaluateSaveReqVO();
        if (CollectorQualityTaskEvaluateSaveReqVO != null) {
            for (CollectorQualityTaskEvaluateSaveReqVO qualityTaskEvaluateSaveReqVO : CollectorQualityTaskEvaluateSaveReqVO) {
                CollectorQualityTaskObjSaveReqVO CollectorQualityTaskObjSaveReqVO1 = collect.get(qualityTaskEvaluateSaveReqVO.getDatasourceId() + qualityTaskEvaluateSaveReqVO.getTableName());
                if (CollectorQualityTaskObjSaveReqVO1 != null) {
                    qualityTaskEvaluateSaveReqVO.setTaskId(dictType.getId());
                    qualityTaskEvaluateSaveReqVO.setObjId(CollectorQualityTaskObjSaveReqVO1.getId());
                    qualityTaskEvaluateSaveReqVO.setObjName(CollectorQualityTaskObjSaveReqVO1.getName());
                    handleCharacterValidationRule(qualityTaskEvaluateSaveReqVO);
                    CollectorQualityTaskEvaluateService.createCollectorQualityTaskEvaluate(qualityTaskEvaluateSaveReqVO);
                }
            }
        }

        return dictType.getId();
    }

    @Override
    public int updateCollectorQualityTask(CollectorQualityTaskSaveReqVO updateReqVO) {
        // 相关校验
        CollectorQualityTaskDO dictType = BeanUtils.toBean(updateReqVO, CollectorQualityTaskDO.class);
        List<CollectorQualityTaskObjSaveReqVO> CollectorQualityTaskObjSaveReqVO = updateReqVO.getCollectorQualityTaskObjSaveReqVO();
        for (CollectorQualityTaskObjSaveReqVO qualityTaskObjSaveReqVO : CollectorQualityTaskObjSaveReqVO) {
            qualityTaskObjSaveReqVO.setTaskId(dictType.getId());
            if (qualityTaskObjSaveReqVO.getId() != null) {
                CollectorQualityTaskObjService.updateCollectorQualityTaskObj(qualityTaskObjSaveReqVO);
            } else {
                Long CollectorQualityTaskObj = CollectorQualityTaskObjService.createCollectorQualityTaskObj(qualityTaskObjSaveReqVO);
                qualityTaskObjSaveReqVO.setId(CollectorQualityTaskObj);
            }
        }
        Map<String, CollectorQualityTaskObjSaveReqVO> collect = CollectorQualityTaskObjSaveReqVO.stream().collect(Collectors.toMap(s -> s.getDatasourceId() + s.getTableName(), Function.identity()));
        List<CollectorQualityTaskEvaluateSaveReqVO> CollectorQualityTaskEvaluateSaveReqVO = updateReqVO.getCollectorQualityTaskEvaluateSaveReqVO();
        if (CollectorQualityTaskEvaluateSaveReqVO != null) {
            for (CollectorQualityTaskEvaluateSaveReqVO qualityTaskEvaluateSaveReqVO : CollectorQualityTaskEvaluateSaveReqVO) {
                CollectorQualityTaskObjSaveReqVO CollectorQualityTaskObjSaveReqVO1 = collect.get(qualityTaskEvaluateSaveReqVO.getDatasourceId() + qualityTaskEvaluateSaveReqVO.getTableName());
                if (CollectorQualityTaskObjSaveReqVO1 != null) {
                    qualityTaskEvaluateSaveReqVO.setObjId(CollectorQualityTaskObjSaveReqVO1.getId());
                    qualityTaskEvaluateSaveReqVO.setObjName(CollectorQualityTaskObjSaveReqVO1.getName());
                }
                handleCharacterValidationRule(qualityTaskEvaluateSaveReqVO);
                if (qualityTaskEvaluateSaveReqVO.getId() != null) {
                    CollectorQualityTaskEvaluateService.updateCollectorQualityTaskEvaluate(qualityTaskEvaluateSaveReqVO);
                } else {
                    qualityTaskEvaluateSaveReqVO.setTaskId(dictType.getId());
                    CollectorQualityTaskEvaluateService.createCollectorQualityTaskEvaluate(qualityTaskEvaluateSaveReqVO);
                }
            }
        }
        return CollectorQualityTaskMapper.updateById(dictType);
    }
    @Override
    public int removeCollectorQualityTask(Collection<Long> idList) {
        // 批量删除数据质量任务
        for (Long id : idList) {
            // 查询 DaDiscoveryTaskDO 详情
            CollectorQualityTaskDO CollectorQualityTaskDO = CollectorQualityTaskMapper.selectById(id);
            if (CollectorQualityTaskDO != null &&
                    (CollectorQualityTaskDO.getSystemJobId() != null || !StringUtils.equals("0",CollectorQualityTaskDO.getTaskCode())) ) {
                // 提取 systemJobId
                if(StringUtils.equals("0",CollectorQualityTaskDO.getStatus())){
                    throw new ServiceException("上线任务，不允删除，请先下线！");
                }
                DsStatusRespDTO dsStatusRespDTO = dsEtlTaskService.deleteTask(projectCode, CollectorQualityTaskDO.getTaskCode());
            }
        }
        return CollectorQualityTaskMapper.deleteBatchIds(idList);
    }

    @Override
    public CollectorQualityTaskRespVO getQualityTaskAsset(CollectorQualityTaskAssetReqVO CollectorQualityTaskAssetReqVO) {
        MPJLambdaWrapper<CollectorQualityTaskDO> wrapper = new MPJLambdaWrapper<>();
        wrapper.selectAll(CollectorQualityTaskDO.class)
                .eq(CollectorQualityTaskDO::getAssetFlag,"1")
                .eq(CollectorQualityTaskDO::getAssetId,CollectorQualityTaskAssetReqVO.getAssetId());
        CollectorQualityTaskDO taskDO = CollectorQualityTaskMapper.selectOne(wrapper);
        if(taskDO == null){
            return null;
        }
        CollectorQualityTaskRespVO CollectorQualityTaskRespVO = buildQualityTaskDetail(taskDO);
        CollectorQualityTaskAssetReqVO.setId(taskDO.getId());
        CollectorQualityLogDO log = CollectorQualityLogService.getCollectorQualityLogById(CollectorQualityTaskAssetReqVO);
        if(log == null){
            // 设置评分与问题数
            CollectorQualityTaskRespVO.setScore(0L);
            CollectorQualityTaskRespVO.setProblemData(0L);
            CollectorQualityTaskRespVO.setLogId(null);
            CollectorQualityTaskRespVO.setLastExecuteTime(null);
            return CollectorQualityTaskRespVO;
        }

        Map<String, Object> map = CollectorEvaluateLogService.sumTotalAndProblemTotalByTaskLogId(String.valueOf(log.getId()));

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
        CollectorQualityTaskRespVO.setScore(score.longValue());
        CollectorQualityTaskRespVO.setProblemData(problemTotal);
        CollectorQualityTaskRespVO.setLogId(log.getId());
        CollectorQualityTaskRespVO.setLastExecuteTime(log.getStartTime());
        return CollectorQualityTaskRespVO;
    }

    @Override
    public CollectorQualityTaskRespVO getCollectorQualityTaskById(Long id) {
        CollectorQualityTaskDO taskDO = CollectorQualityTaskMapper.selectById(id);
        return taskDO != null ? buildQualityTaskDetail(taskDO) : null;
    }

    private CollectorQualityTaskRespVO buildQualityTaskDetail(CollectorQualityTaskDO CollectorQualityTaskDO) {
        CollectorQualityTaskRespVO bean = BeanUtils.toBean(CollectorQualityTaskDO, CollectorQualityTaskRespVO.class);

        // 数据对象列表
        LambdaQueryWrapperX<CollectorQualityTaskObjDO> objectLambdaQueryWrapperX = new LambdaQueryWrapperX<>();
        objectLambdaQueryWrapperX.eq(CollectorQualityTaskObjDO::getTaskId , CollectorQualityTaskDO.getId());
        List<CollectorQualityTaskObjDO> list = CollectorQualityTaskObjService.list(objectLambdaQueryWrapperX);

        List<CollectorQualityTaskObjRespVO> newList = new ArrayList<>();
        for (CollectorQualityTaskObjDO obj : list) {
            AssetsDatasourceRespDTO ds = daDatasourceApiService.getDatasourceById(obj.getDatasourceId());
            CollectorQualityTaskObjRespVO vo = BeanUtils.toBean(obj, CollectorQualityTaskObjRespVO.class);
            if (ds != null) {
                vo.setDatasourceType(ds.getDatasourceType());
                vo.setDatasourceConfig(ds.getDatasourceConfig());
            }
            newList.add(vo);
        }

        // 规则列表
        LambdaQueryWrapperX<CollectorQualityTaskEvaluateDO> evaWrapper = new LambdaQueryWrapperX<>();
        evaWrapper.eq(CollectorQualityTaskEvaluateDO::getTaskId , CollectorQualityTaskDO.getId());
        List<CollectorQualityTaskEvaluateDO> evaList = CollectorQualityTaskEvaluateService.list(evaWrapper);

        List<CollectorQualityTaskEvaluateRespVO> evaRespList = new ArrayList<>();
        for (CollectorQualityTaskEvaluateDO eva : evaList) {
            handleCharacterValidationRule(eva);
            evaRespList.add(BeanUtils.toBean(eva, CollectorQualityTaskEvaluateRespVO.class));
        }

        bean.setCollectorQualityTaskObjSaveReqVO(newList);
        bean.setCollectorQualityTaskEvaluateRespVOS(evaRespList);
        return bean;
    }

    public CollectorQualityTaskRespVO getDaDiscoveryTaskById(Long id) {

        MPJLambdaWrapper<CollectorQualityTaskDO> mpjLambdaWrapper = new MPJLambdaWrapper();
        mpjLambdaWrapper.selectAll(CollectorQualityTaskDO.class)
                .select("t2.name AS catName")
                .leftJoin("TAX_QUALITY_CAT t2 on t.CAT_CODE = t2.CODE AND t2.DEL_FLAG = '0'")
                .eq(CollectorQualityTaskDO::getId, id);
        CollectorQualityTaskDO daDiscoveryTaskDO =  CollectorQualityTaskMapper.selectJoinOne(CollectorQualityTaskDO.class, mpjLambdaWrapper);

        CollectorQualityTaskRespVO bean = BeanUtils.toBean(daDiscoveryTaskDO, CollectorQualityTaskRespVO.class);


//        DaDatasourceRespDTO daDatasourceById = daDatasourceApiService.getDatasourceById(bean.getDatasourceId());
//        daDatasourceById = daDatasourceById == null ? new DaDatasourceRespDTO():daDatasourceById;
//        bean.setDatasourceName(daDatasourceById.getDatasourceName());
//        bean.setDatasourceType(daDatasourceById.getDatasourceType());
//        bean.setIp(daDatasourceById.getIp());
//
//        List<CollectorQualityTaskObjDO> daDiscoveryTableDOList = fetchDiscoveryTableList(bean);
//        daDiscoveryTableDOList = daDiscoveryTableDOList == null ? new ArrayList<>():daDiscoveryTableDOList;


//        long countPending = daDiscoveryTableDOList.stream()
//                .filter(item -> StringUtils.equals("1",item.get()))
//                .count();
//
//        long countSubmitted = daDiscoveryTableDOList.stream()
//                .filter(item -> StringUtils.equals("2",item.getStatus()))
//                .count();
//
//        //0:否，1：是
//        long countIgnoreFlag = daDiscoveryTableDOList.stream()
//                .filter(item -> StringUtils.equals("1",item.getIgnoreFlag()))
//                .count();
//        bean.setCountPending(countPending);
//        bean.setCountSubmitted(countSubmitted);
//        bean.setCountIgnoreFlag(countIgnoreFlag);


//        Long systemJobId = bean.getSystemJobId();
//        SysJob sysJob = iSysJobService.selectJobById(systemJobId);
//        sysJob = sysJob == null ? new SysJob():sysJob;
//        bean.setMisfirePolicy(sysJob.getMisfirePolicy());
//        bean.setJobGroup(sysJob.getJobGroup());
//        bean.setConcurrent(sysJob.getConcurrent());


        return bean;
    }

    private List<CollectorQualityTaskObjDO> fetchDiscoveryTableList(CollectorQualityTaskRespVO daDiscoveryTaskDO) {
        LambdaQueryWrapperX<CollectorQualityTaskObjDO> objectLambdaQueryWrapperX = new LambdaQueryWrapperX<>();
        objectLambdaQueryWrapperX.eqIfPresent(CollectorQualityTaskObjDO::getTaskId , daDiscoveryTaskDO.getId());
        return CollectorQualityTaskObjService.list(objectLambdaQueryWrapperX);
    }

    @Override
    public List<CollectorQualityTaskDO> getCollectorQualityTaskList() {
        return CollectorQualityTaskMapper.selectList();
    }

    @Override
    public Map<Long, CollectorQualityTaskDO> getCollectorQualityTaskMap() {
        List<CollectorQualityTaskDO> CollectorQualityTaskList = CollectorQualityTaskMapper.selectList();
        return CollectorQualityTaskList.stream()
                .collect(Collectors.toMap(
                        CollectorQualityTaskDO::getId,
                        CollectorQualityTaskDO -> CollectorQualityTaskDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing
                ));
    }


        /**
         * 导入数据质量任务数据
         *
         * @param importExcelList 数据质量任务数据列表
         * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
         * @param operName 操作用户
         * @return 结果
         */
        @Override
        public String importCollectorQualityTask(List<CollectorQualityTaskRespVO> importExcelList, boolean isUpdateSupport, String operName) {
            if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
                throw new ServiceException("导入数据不能为空！");
            }

            int successNum = 0;
            int failureNum = 0;
            List<String> successMessages = new ArrayList<>();
            List<String> failureMessages = new ArrayList<>();

            for (CollectorQualityTaskRespVO respVO : importExcelList) {
                try {
                    CollectorQualityTaskDO CollectorQualityTaskDO = BeanUtils.toBean(respVO, CollectorQualityTaskDO.class);
                    Long CollectorQualityTaskId = respVO.getId();
                    if (isUpdateSupport) {
                        if (CollectorQualityTaskId != null) {
                            CollectorQualityTaskDO existingCollectorQualityTask = CollectorQualityTaskMapper.selectById(CollectorQualityTaskId);
                            if (existingCollectorQualityTask != null) {
                                CollectorQualityTaskMapper.updateById(CollectorQualityTaskDO);
                                successNum++;
                                successMessages.add("数据更新成功，ID为 " + CollectorQualityTaskId + " 的数据质量任务记录。");
                            } else {
                                failureNum++;
                                failureMessages.add("数据更新失败，ID为 " + CollectorQualityTaskId + " 的数据质量任务记录不存在。");
                            }
                        } else {
                            failureNum++;
                            failureMessages.add("数据更新失败，某条记录的ID不存在。");
                        }
                    } else {
                        QueryWrapper<CollectorQualityTaskDO> queryWrapper = new QueryWrapper<>();
                        queryWrapper.eq("id", CollectorQualityTaskId);
                        CollectorQualityTaskDO existingCollectorQualityTask = CollectorQualityTaskMapper.selectOne(queryWrapper);
                        if (existingCollectorQualityTask == null) {
                            CollectorQualityTaskMapper.insert(CollectorQualityTaskDO);
                            successNum++;
                            successMessages.add("数据插入成功，ID为 " + CollectorQualityTaskId + " 的数据质量任务记录。");
                        } else {
                            failureNum++;
                            failureMessages.add("数据插入失败，ID为 " + CollectorQualityTaskId + " 的数据质量任务记录已存在。");
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
    public String verifyInterfaceValue(CollectorQualityTaskEvaluateSaveReqVO CollectorQualityTaskEvaluate) {
        // 处理正则
//        JSONObject jsonObject = JSONObject.parseObject(CollectorQualityTaskEvaluate.getRule());
//        List<String> lists = jsonObject.getList("allowedChars", String.class);
//        String s = this.validateInputWithRegex(lists);

        Map<String, Object> map = this.buildRuleParamMap(CollectorQualityTaskEvaluate);
        map.put("dataId", CollectorQualityTaskEvaluate.getDatasourceId());
        map.put("inputValue", CollectorQualityTaskEvaluate.getTitle());
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
                return CollectorQualityTaskEvaluate.getTitle() + "，数据监测成功";
            }
            return CollectorQualityTaskEvaluate.getTitle() + "，不符合规则";
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public AjaxResult startCollectorQualityTask(Long id) {
        CollectorQualityTaskDO CollectorQualityTaskDO = CollectorQualityTaskMapper.selectById(id);
        if(CollectorQualityTaskDO == null){
            return error("任务不存在，请刷新后重试！");
        }
        if (!StringUtils.equals("0",CollectorQualityTaskDO.getStatus())){
            return error("任务状态错误，请刷新后重试！");
        }

        DsStartTaskReqDTO dsStartTaskReqDTO = CollectorTaskConverter.createDsStartTaskReqDTO(CollectorQualityTaskDO.getTaskCode());

        DsStatusRespDTO dsStatusRespDTO = dsEtlTaskService.startTask(dsStartTaskReqDTO, projectCode);

        return dsStatusRespDTO.getSuccess() ? success() : error(dsStatusRespDTO.getMsg());
    }

    @Override
    public boolean updateCollectorQualityTaskStatus(CollectorQualityTaskSaveReqVO daDiscoveryTask) {
        CollectorQualityTaskRespVO CollectorQualityTaskById = this.getDaDiscoveryTaskById(daDiscoveryTask.getId());
        String daDiscoveryTaskStatus = daDiscoveryTask.getStatus();

        validateTaskStatus(CollectorQualityTaskById, daDiscoveryTaskStatus);

        daDiscoveryTask.setCycle(CollectorQualityTaskById.getCycle());
        Long systemJobId = CollectorQualityTaskById.getSystemJobId();
        if (StringUtils.equals(daDiscoveryTaskStatus, CollectorQualityTaskById.getStatus())) {
            return true;
        }

        if (StringUtils.equals("1", daDiscoveryTaskStatus)) {
            handleOfflineTask(CollectorQualityTaskById, systemJobId, daDiscoveryTask);
            return true;
        }

        handleOnlineTask(CollectorQualityTaskById, systemJobId, daDiscoveryTask);

        updateTaskStatusAndScheduler(daDiscoveryTask, systemJobId);

        return true;
    }

    @Override
    public JSONObject validationErrorDataSql(CollectorQualityTaskEvaluateSaveReqVO CollectorQualityTaskEvaluate) {
        Map<String, Object> objectObjectHashMap =  this.buildRuleParamMap(CollectorQualityTaskEvaluate);
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
    public JSONObject validationValidDataSql(CollectorQualityTaskEvaluateSaveReqVO CollectorQualityTaskEvaluate) {
        Map<String, Object> objectObjectHashMap =  this.buildRuleParamMap(CollectorQualityTaskEvaluate);
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
    public boolean updateDaDiscoveryTaskCronExpression(CollectorQualityTaskSaveReqVO daDiscoveryTask) {
        CollectorQualityTaskRespVO CollectorQualityTaskById = this.getCollectorQualityTaskById(daDiscoveryTask.getId());
        Long systemJobId = CollectorQualityTaskById.getSystemJobId();
        if(systemJobId != null){
            try {
                //     * 创建调度器 (只有任务发布了才能调用该接口)
                DsSchedulerUpdateReqDTO schedulerUpdateRequest = CollectorTaskConverter.createSchedulerUpdateRequest(systemJobId, daDiscoveryTask.getCycle(), CollectorQualityTaskById.getTaskCode());
                DsSchedulerRespDTO dsSchedulerRespDTO = iDsEtlSchedulerService.updateScheduler(schedulerUpdateRequest, String.valueOf(projectCode));
                if(dsSchedulerRespDTO == null || !dsSchedulerRespDTO.getSuccess()){
                    daDiscoveryTask.setTaskId(CollectorQualityTaskById.getTaskId());
                    daDiscoveryTask.setTaskCode(String.valueOf(CollectorQualityTaskById.getTaskCode()));
                    daDiscoveryTask.setNodeId(CollectorQualityTaskById.getNodeId());
                    daDiscoveryTask.setNodeCode(String.valueOf(CollectorQualityTaskById.getNodeCode()));
                    createSchedulerIfNeeded(daDiscoveryTask);
                }else {
                    Schedule schedule = dsSchedulerRespDTO.getData();
                    daDiscoveryTask.setSystemJobId(schedule.getId());
                }
            } catch (Exception e){
                throw new ServiceException("调度周期修改失败，请联系系统管理员！");

            }
        }

        // 更新数据发现任务
        CollectorQualityTaskDO updateObj = BeanUtils.toBean(daDiscoveryTask, CollectorQualityTaskDO.class);
        CollectorQualityTaskMapper.updateById(updateObj);
//        this.updateDaDiscoveryTask(daDiscoveryTask);
        return true;
    }

    private void validateTaskStatus(CollectorQualityTaskRespVO daDiscoveryTaskById, String daDiscoveryTaskStatus) {
        if (daDiscoveryTaskById == null || daDiscoveryTaskStatus == null) {
            throw new ServiceException("任务模版错误，未查询到调度信息！");
        }
    }

    private void handleOfflineTask(CollectorQualityTaskRespVO daDiscoveryTaskById, Long systemJobId, CollectorQualityTaskSaveReqVO daDiscoveryTask) {
        if(daDiscoveryTaskById.getSystemJobId() != null &&  systemJobId > 0){
            DsStatusRespDTO respDTO = dsEtlTaskService.releaseTask("OFFLINE", String.valueOf(projectCode), daDiscoveryTaskById.getTaskCode());
            if (respDTO == null || !respDTO.getSuccess()) {
                throw new ServiceException("发布或下线任务，失败！");
            }

            DsStatusRespDTO offlined = iDsEtlSchedulerService.offlineScheduler(projectCode, systemJobId);
            if (!offlined.getData()) {
                throw new ServiceException("下线调度器，失败！");
            }
        }

        // 更新数据发现任务
        CollectorQualityTaskDO updateObj = BeanUtils.toBean(daDiscoveryTask, CollectorQualityTaskDO.class);
        CollectorQualityTaskMapper.updateById(updateObj);
    }

    private void handleOnlineTask(CollectorQualityTaskRespVO daDiscoveryTaskById, Long systemJobId, CollectorQualityTaskSaveReqVO daDiscoveryTask) {
        if (systemJobId == null || systemJobId < 1) {
            createNewProcessDefinition(daDiscoveryTaskById, daDiscoveryTask);
        } else if (daDiscoveryTaskById.getId() != null) {
            updateExistingProcessDefinition(daDiscoveryTaskById, daDiscoveryTask);
        }
    }

    private void createNewProcessDefinition(CollectorQualityTaskRespVO daDiscoveryTaskById, CollectorQualityTaskSaveReqVO daDiscoveryTask) {
        TaskSaveReqInput input = new TaskSaveReqInput();
        input.setName(daDiscoveryTaskById.getTaskName() + StringUtils.generateRandomString());
        input.addHttpParam("id", "BODY", daDiscoveryTaskById.getId());
        input.setId(daDiscoveryTaskById.getId());
        ProcessDefinition definition = this.createProcessDefinition(input);
        TaskDefinition firstTaskDefinition = CollectorTaskConverter.getFirstTaskDefinition(definition);

        daDiscoveryTask.setTaskId(definition.getId());
        daDiscoveryTask.setTaskCode(String.valueOf(definition.getCode()));
        daDiscoveryTask.setNodeId(firstTaskDefinition.getId());
        daDiscoveryTask.setNodeCode(String.valueOf(firstTaskDefinition.getCode()));
    }

    private void updateExistingProcessDefinition(CollectorQualityTaskRespVO daDiscoveryTaskById, CollectorQualityTaskSaveReqVO daDiscoveryTask) {
        TaskSaveReqInput input = new TaskSaveReqInput();
        input.setName(daDiscoveryTaskById.getTaskName() + StringUtils.generateRandomString());
        input.addHttpParam("id", "BODY", daDiscoveryTaskById.getId());
        input.setId(daDiscoveryTaskById.getId());

        input.setTaskId(daDiscoveryTaskById.getTaskId());
        input.setTaskCode(String.valueOf(daDiscoveryTaskById.getTaskCode()));
        input.setNodeId(daDiscoveryTaskById.getNodeId());
        input.setNodeCode(String.valueOf(daDiscoveryTaskById.getNodeCode()));

        ProcessDefinition definition = this.updateProcessDefinition(input);
        TaskDefinition firstTaskDefinition = CollectorTaskConverter.getFirstTaskDefinition(definition);

        daDiscoveryTask.setTaskId(definition.getId());
        daDiscoveryTask.setTaskCode(String.valueOf(definition.getCode()));
        daDiscoveryTask.setNodeId(firstTaskDefinition.getId());
        daDiscoveryTask.setNodeCode(String.valueOf(firstTaskDefinition.getCode()));
    }


    private void updateTaskStatusAndScheduler(CollectorQualityTaskSaveReqVO daDiscoveryTask, Long systemJobId) {
        DsStatusRespDTO dsStatusRespDTO = dsEtlTaskService.releaseTask("ONLINE", String.valueOf(projectCode), daDiscoveryTask.getTaskCode());
        if (dsStatusRespDTO == null || !dsStatusRespDTO.getSuccess()) {
            throw new ServiceException("发布或下线任务，失败！");
        }

        if (systemJobId != null && systemJobId > 0) {
            updateExistingScheduler(daDiscoveryTask, systemJobId);
        } else {
            createNewScheduler(daDiscoveryTask);
        }

        DsStatusRespDTO dsStatusRespDTO1 = iDsEtlSchedulerService.onlineScheduler(projectCode, daDiscoveryTask.getSystemJobId());
        if (!dsStatusRespDTO1.getData()) {
            throw new ServiceException("上线调度器，失败！");
        }

        // 更新数据发现任务
        CollectorQualityTaskDO updateObj = BeanUtils.toBean(daDiscoveryTask, CollectorQualityTaskDO.class);
        CollectorQualityTaskMapper.updateById(updateObj);
    }


    private void updateExistingScheduler(CollectorQualityTaskSaveReqVO daDiscoveryTask, Long systemJobId) {
        DsSchedulerUpdateReqDTO schedulerUpdateRequest = CollectorTaskConverter.createSchedulerUpdateRequest(systemJobId, daDiscoveryTask.getCycle(), daDiscoveryTask.getTaskCode());
        DsSchedulerRespDTO dsSchedulerRespDTO = iDsEtlSchedulerService.updateScheduler(schedulerUpdateRequest, String.valueOf(projectCode));
        if (dsSchedulerRespDTO == null || !dsSchedulerRespDTO.getSuccess()) {
            createSchedulerIfNeeded(daDiscoveryTask);
        } else {
            Schedule schedule = dsSchedulerRespDTO.getData();
            daDiscoveryTask.setSystemJobId(schedule.getId());
        }
    }

    private void createNewScheduler(CollectorQualityTaskSaveReqVO daDiscoveryTask) {
        DsSchedulerSaveReqDTO dsSchedulerSaveReqDTO = CollectorTaskConverter.createSchedulerRequest(daDiscoveryTask.getCycle(), daDiscoveryTask.getTaskCode());
        DsSchedulerRespDTO dsSchedulerRespDTO = iDsEtlSchedulerService.saveScheduler(dsSchedulerSaveReqDTO, String.valueOf(projectCode));
        if (dsSchedulerRespDTO == null || !dsSchedulerRespDTO.getSuccess()) {
            createSchedulerIfNeeded(daDiscoveryTask);
        } else {
            Schedule schedule = dsSchedulerRespDTO.getData();
            daDiscoveryTask.setSystemJobId(schedule.getId());
        }
    }


    private void createSchedulerIfNeeded(CollectorQualityTaskSaveReqVO daDiscoveryTask) {
        DsSchedulerRespDTO byTaskCode = iDsEtlSchedulerService.getByTaskCode(String.valueOf(projectCode), daDiscoveryTask.getTaskCode());
        if (byTaskCode == null || !byTaskCode.getSuccess()) {
            //     * 创建调度器 (只有任务发布了才能调用该接口)
            DsSchedulerSaveReqDTO dsSchedulerSaveReqDTO = CollectorTaskConverter.createSchedulerRequest(daDiscoveryTask.getCycle(),daDiscoveryTask.getTaskCode());
            DsSchedulerRespDTO saveScheduler = iDsEtlSchedulerService.saveScheduler(dsSchedulerSaveReqDTO, String.valueOf(projectCode));
            if(saveScheduler == null || !saveScheduler.getSuccess()){
                throw new ServiceException("创建调度器，失败！");
            }
            Schedule schedule = saveScheduler.getData();

            daDiscoveryTask.setSystemJobId(schedule.getId());
            return;
        }
        Schedule schedule = byTaskCode.getData();
        daDiscoveryTask.setSystemJobId(schedule.getId());
        DsSchedulerUpdateReqDTO schedulerUpdateRequest = CollectorTaskConverter.createSchedulerUpdateRequest(schedule.getId(), daDiscoveryTask.getCycle(), daDiscoveryTask.getTaskCode());
        DsSchedulerRespDTO updated = iDsEtlSchedulerService.updateScheduler(schedulerUpdateRequest, String.valueOf(projectCode));
        if (updated == null || !updated.getSuccess()) {
            throw new ServiceException("更新调度器，失败！");
        }
    }

    public ProcessDefinition updateProcessDefinition(TaskSaveReqInput input) {
        Long nodeUniqueKey = this.getNodeUniqueKey(CollectorTaskConverter.stringToLong(projectCode));

        input.setNodeCode(CollectorTaskConverter.longToString(nodeUniqueKey));

        DsTaskSaveReqDTO dsTaskSaveReqDTO = CollectorTaskConverter.buildDsTaskSaveReq(input);
        DsTaskSaveRespDTO task = dsEtlTaskService.updateTask(dsTaskSaveReqDTO,projectCode,input.getTaskCode() );

        if (!task.getSuccess()) {
            throw new ServiceException("任务状态修改失败，请联系系统管理员"); // 抛出任务定义创建错误的异常
        }
        ProcessDefinition data = task.getData();
        return data; // 返回创建结果
    }

    public ProcessDefinition createProcessDefinition(TaskSaveReqInput input) {
        Long nodeUniqueKey = this.getNodeUniqueKey(CollectorTaskConverter.stringToLong(projectCode));

        input.setNodeCode(CollectorTaskConverter.longToString(nodeUniqueKey));

        DsTaskSaveReqDTO dsTaskSaveReqDTO = CollectorTaskConverter.buildDsTaskSaveReq(input);
        DsTaskSaveRespDTO task = dsEtlTaskService.createTask(dsTaskSaveReqDTO,CollectorTaskConverter.stringToLong(projectCode) );

        if (!task.getSuccess()) {
            throw new ServiceException("任务状态修改失败，请联系系统管理员"); // 抛出任务定义创建错误的异常
        }
        ProcessDefinition data = task.getData();
        return data; // 返回创建结果
    }

    public Long getNodeUniqueKey(Long projectCode) {
        try {
            DsNodeGenCodeRespDTO dsNodeGenCodeRespDTO = dsEtlNodeService.genCode(projectCode);
            return dsNodeGenCodeRespDTO.getData().get(0);
        } catch (Exception e){
            throw new ServiceException("任务状态修改失败，请联系系统管理员"); // 抛出任务定义创建错误的异常
        }
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
     * @param CollectorQualityTaskEvaluate
     * @return
     */
    public static Map<String, Object> buildRuleParamMap(CollectorQualityTaskEvaluateSaveReqVO CollectorQualityTaskEvaluate) {
        Map<String, Object> paramMap = new HashMap<>();

        // 1. 数据源 ID
        paramMap.put("dataId", CollectorQualityTaskEvaluate.getDatasourceId());

        // 2. 表名
        paramMap.put("tableName", CollectorQualityTaskEvaluate.getTableName());

        // 3. 规则类型
        paramMap.put("ruleType", CollectorQualityTaskEvaluate.getRuleType());

        // 4. 分页信息（临时写死 ruleType，如后续有分页参数可调整）
        paramMap.put("pageNum", CollectorQualityTaskEvaluate.getPageNum());
        paramMap.put("pageSize", CollectorQualityTaskEvaluate.getPageSize());


        String stringObjectMap = buildCharacterValidationRule(CollectorQualityTaskEvaluate.getRule(), CollectorQualityTaskEvaluate.getRuleType());

        // 5. 规则配置
        paramMap.put("config",  JSONUtils.convertTaskDefinitionJsonMap(stringObjectMap));

        // 6. 评估字段
        paramMap.put("evaColumn", CollectorQualityTaskEvaluate.getEvaColumn());

        // 7. where 条件
        paramMap.put("whereClause", CollectorQualityTaskEvaluate.getWhereClause());

        return paramMap;
    }
    /**
     * 处理 CHARACTER_VALIDATION 规则
     * 兼容 SaveReqVO 与 DO 两种类型
     */
    public static void handleCharacterValidationRule(CollectorQualityTaskEvaluateSaveReqVO qualityTaskEvaluateSaveReqVO) {
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

    public static void handleCharacterValidationRule(CollectorQualityTaskEvaluateDO evaluateDO) {
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
        return baseMapper.selectCount(Wrappers.lambdaQuery(CollectorQualityTaskDO.class)
                .likeRight(CollectorQualityTaskDO::getCatCode, catCode));
    }

}
