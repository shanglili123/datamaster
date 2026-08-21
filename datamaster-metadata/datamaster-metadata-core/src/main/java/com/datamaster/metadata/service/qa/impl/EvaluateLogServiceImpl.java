

package com.datamaster.metadata.service.qa.impl;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.datamaster.common.datasource.mgmt.api.dto.DatasourceRespDTO;
import lombok.extern.slf4j.Slf4j;
import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.core.text.Convert;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.httpClient.HeaderEntity;
import com.datamaster.common.httpClient.HttpUtils;
import com.datamaster.common.utils.DateUtils;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.common.utils.object.BeanUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.datamaster.common.datasource.mgmt.api.IDatasourceApiService;
import com.datamaster.metadata.controller.qa.vo.*;
import com.datamaster.metadata.controller.qa.vo.*;
import com.datamaster.metadata.dal.dataobject.qa.EvaluateLogDO;
import com.datamaster.metadata.dal.dataobject.qa.QualityLogDO;
import com.datamaster.metadata.dal.dataobject.qa.QualityTaskEvaluateDO;
import com.datamaster.metadata.dal.dataobject.qa.QualityTaskObjDO;
import com.datamaster.metadata.dal.mapper.qa.EvaluateLogMapper;
import com.datamaster.metadata.service.qa.IEvaluateLogService;
import com.datamaster.metadata.service.qa.IQualityLogService;
import com.datamaster.metadata.service.qa.IQualityTaskEvaluateService;
import com.datamaster.metadata.service.qa.IQualityTaskObjService;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

/**
 * 评测规则结果Service业务层处理
 *
 * @author lili.shang
 * @date 2025-07-21
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class EvaluateLogServiceImpl  extends ServiceImpl<EvaluateLogMapper,EvaluateLogDO> implements IEvaluateLogService {

    @Value("${path.quality_url}")
    private String url;
    @Resource
    private EvaluateLogMapper EvaluateLogMapper;

    @Resource
    @Lazy
    private IQualityLogService QualityLogService;
    @Resource
    @Lazy
    private IQualityTaskEvaluateService QualityTaskEvaluateService;
    @Resource
    @Lazy
    private IQualityTaskObjService QualityTaskObjService;
    @Resource
    @Lazy
    private IDatasourceApiService daDatasourceApiService;

    @Override
    public PageResult<EvaluateLogDO> getEvaluateLogPage(EvaluateLogPageReqVO pageReqVO) {
        return EvaluateLogMapper.selectPage(pageReqVO);
    }

    @Override
    public Long createEvaluateLog(EvaluateLogSaveReqVO createReqVO) {
        EvaluateLogDO dictType = BeanUtils.toBean(createReqVO, EvaluateLogDO.class);
        EvaluateLogMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public int updateEvaluateLog(EvaluateLogSaveReqVO updateReqVO) {
        // 相关校验

        // 更新评测规则结果
        EvaluateLogDO updateObj = BeanUtils.toBean(updateReqVO, EvaluateLogDO.class);
        return EvaluateLogMapper.updateById(updateObj);
    }
    @Override
    public int removeEvaluateLog(Collection<Long> idList) {
        // 批量删除评测规则结果
        return EvaluateLogMapper.deleteBatchIds(idList);
    }

    @Override
    public EvaluateLogDO getEvaluateLogById(Long id) {
        return EvaluateLogMapper.selectById(id);
    }

    @Override
    public List<EvaluateLogDO> getEvaluateLogList() {
        return EvaluateLogMapper.selectList();
    }

    @Override
    public Map<Long, EvaluateLogDO> getEvaluateLogMap() {
        List<EvaluateLogDO> EvaluateLogList = EvaluateLogMapper.selectList();
        return EvaluateLogList.stream()
                .collect(Collectors.toMap(
                        EvaluateLogDO::getId,
                        EvaluateLogDO -> EvaluateLogDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing
                ));
    }

    @Override
    public Map<String, Object> sumTotalAndProblemTotalByTaskLogId(String taskLogId) {
        List<EvaluateLogDO> list = EvaluateLogMapper.selectList(new LambdaQueryWrapperX<EvaluateLogDO>()
                .eq(EvaluateLogDO::getTaskLogId, taskLogId)
                .eq(EvaluateLogDO::getValidFlag, "1")); // 如有需要加条件

        Long total = list.stream()
                .mapToLong(log -> log.getTotal() == null ? 0L : log.getTotal())
                .sum();

        Long problemTotal = list.stream()
                .mapToLong(log -> log.getProblemTotal() == null ? 0L : log.getProblemTotal())
                .sum();

        Map<String, Object> summary = new HashMap<>();
        summary.put("total", total);
        summary.put("problemTotal", problemTotal);
        return summary;
    }


    /**
     * 导入评测规则结果数据
     *
     * @param importExcelList 评测规则结果数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName 操作用户
     * @return 结果
     */
    @Override
    public String importEvaluateLog(List<EvaluateLogRespVO> importExcelList, boolean isUpdateSupport, String operName) {
        if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
            throw new ServiceException("导入数据不能为空！");
        }

        int successNum = 0;
        int failureNum = 0;
        List<String> successMessages = new ArrayList<>();
        List<String> failureMessages = new ArrayList<>();

        for (EvaluateLogRespVO respVO : importExcelList) {
            try {
                EvaluateLogDO EvaluateLogDO = BeanUtils.toBean(respVO, EvaluateLogDO.class);
                Long EvaluateLogId = respVO.getId();
                if (isUpdateSupport) {
                    if (EvaluateLogId != null) {
                        EvaluateLogDO existingEvaluateLog = EvaluateLogMapper.selectById(EvaluateLogId);
                        if (existingEvaluateLog != null) {
                            EvaluateLogMapper.updateById(EvaluateLogDO);
                            successNum++;
                            successMessages.add("数据更新成功，ID为 " + EvaluateLogId + " 的评测规则结果记录。");
                        } else {
                            failureNum++;
                            failureMessages.add("数据更新失败，ID为 " + EvaluateLogId + " 的评测规则结果记录不存在。");
                        }
                    } else {
                        failureNum++;
                        failureMessages.add("数据更新失败，某条记录的ID不存在。");
                    }
                } else {
                    QueryWrapper<EvaluateLogDO> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("id", EvaluateLogId);
                    EvaluateLogDO existingEvaluateLog = EvaluateLogMapper.selectOne(queryWrapper);
                    if (existingEvaluateLog == null) {
                        EvaluateLogMapper.insert(EvaluateLogDO);
                        successNum++;
                        successMessages.add("数据插入成功，ID为 " + EvaluateLogId + " 的评测规则结果记录。");
                    } else {
                        failureNum++;
                        failureMessages.add("数据插入失败，ID为 " + EvaluateLogId + " 的评测规则结果记录已存在。");
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
    public List<EvaluateLogStatisticsVO> statisticsEvaluateOne(Long id) {
        List<EvaluateLogStatisticsVO> CollectorEvaluateDimStatVOS = EvaluateLogMapper.selectDimStatsByTaskLogId(String.valueOf(id));
        if(CollectorEvaluateDimStatVOS.isEmpty()){
            return new ArrayList<>();
        }
        QualityLogDO QualityLogDO = QualityLogService.selectPrevLogByIdWithWrapper(id);
        if(QualityLogDO == null){
            for (EvaluateLogStatisticsVO vo : CollectorEvaluateDimStatVOS) {
                vo.setTrendType(3L);
            }
            return CollectorEvaluateDimStatVOS;
        }
        List<EvaluateLogStatisticsVO> prevList = EvaluateLogMapper.selectDimStatsByTaskLogId(String.valueOf(QualityLogDO.getId()));
        if(prevList == null || prevList.isEmpty()){
            for (EvaluateLogStatisticsVO vo : CollectorEvaluateDimStatVOS) {
                vo.setTrendType(3L);
            }
            return CollectorEvaluateDimStatVOS;
        }

        // 4) 以维度为 Key 的上次“问题占比”基线
        Map<String, BigDecimal> prevProportionMap = new HashMap<>(prevList.size() * 2);
        for (EvaluateLogStatisticsVO vo : prevList) {
            BigDecimal val = vo.getProportion() == null ? BigDecimal.ZERO : vo.getProportion();
            prevProportionMap.put(String.valueOf(vo.getDimensionType()), val);
        }

        for (EvaluateLogStatisticsVO vo : CollectorEvaluateDimStatVOS) {
            String dim = String.valueOf(vo.getDimensionType());
            BigDecimal curProportion = vo.getProportion() == null ? BigDecimal.ZERO : vo.getProportion();
            BigDecimal prevProportion = prevProportionMap.get(dim);

            if (prevProportion == null) {
                vo.setTrendType(3L);
                continue;
            }

            int cmp = curProportion.compareTo(prevProportion);
            if (cmp > 0) {
                vo.setTrendType(1L);
            } else if (cmp < 0) {
                vo.setTrendType(2L);
            } else {
                vo.setTrendType(3L);
            }
        }
        return CollectorEvaluateDimStatVOS;
    }

//    @Override
//    public List<EvaluateLogStatisticsVO> statisticsEvaluateOne(Long id) {
//            List<EvaluateLogStatisticsVO> voList = new ArrayList<>();
//        QualityLogDO QualityLogById = QualityLogService.getQualityLogById(id);
//        QualityLogPageReqVO QualityLogPageReqVO = new QualityLogPageReqVO();
//        QualityLogPageReqVO.setQualityId(QualityLogById.getQualityId());
//        PageResult<QualityLogDO> QualityLogPage = QualityLogService.getQualityLogPage(QualityLogPageReqVO);
//        List<QualityLogDO> rows =  (List<QualityLogDO>) QualityLogPage.getRows();
//        QualityLogPageReqVO old = new QualityLogPageReqVO();
//        for (QualityLogDO row : rows) {
//            if (row.getCreateTime().getTime() <= QualityLogById.getCreateTime().getTime()) {
//                old = BeanUtils.toBean(row , QualityLogPageReqVO.class);
//                break;
//            }
//        }
//        // 最新的
//        Long problemTotalAll = 0L;
//        List<EvaluateLogDO> taskLogId = EvaluateLogMapper.selectList("task_log_id", id);
//        Map<String, List<EvaluateLogDO>> collect = taskLogId.stream().collect(Collectors.groupingBy(s -> s.getDimensionType()));
//        for (EvaluateLogDO aDo : taskLogId) {
//            problemTotalAll += aDo.getProblemTotal();
//        }
//
//        // 老的
//        List<EvaluateLogDO> oldTaskLogId = EvaluateLogMapper.selectList("task_log_id", old.getId());
//        Map<String, List<EvaluateLogDO>> oldCollect = oldTaskLogId.stream().collect(Collectors.groupingBy(s -> s.getDimensionType()));
//        for (EvaluateLogDO aDo : oldTaskLogId) {
//
//        }
//        for (Map.Entry<String, List<EvaluateLogDO>> entry : collect.entrySet()) {
//            EvaluateLogStatisticsVO vo = new EvaluateLogStatisticsVO();
//            List<EvaluateLogDO> value = entry.getValue();
//            Long problemTotal = 0L;
//            for (EvaluateLogDO EvaluateLogDO : value) {
//                problemTotal += EvaluateLogDO.getProblemTotal();
//            }
//            vo.setDimensionType(entry.getKey());
//            vo.setProblemTotal(problemTotal);
//            vo.setSuccesTotal(Convert.toLong(value.size()));
//            vo.setProportion(BigDecimal.ZERO);
//            if (!problemTotal.equals(0L) && !problemTotalAll.equals(0L)) {
//                BigDecimal bigDecimal = new BigDecimal(problemTotal).divide(new BigDecimal(problemTotalAll) , 5, RoundingMode.HALF_UP);
//                BigDecimal subtract = bigDecimal.multiply(new BigDecimal(100));
//                vo.setProportion(subtract.setScale(2 , RoundingMode.HALF_UP));
//            }
//            List<EvaluateLogDO> EvaluateLogDOS = oldCollect.get(entry.getKey());
//            Long oldProblemTotal = 0L;
//            if (EvaluateLogDOS != null) {
//                for (EvaluateLogDO EvaluateLogDO : EvaluateLogDOS) {
//                    oldProblemTotal += EvaluateLogDO.getProblemTotal();
//                }
//                // 趋势 0：下降，1：上升
//                if (problemTotal > oldProblemTotal) {
//                    vo.setTrendType(0L);
//                } else {
//                    vo.setTrendType(1L);
//                }
//            }
//
//            voList.add(vo);
//        }
//
//        return voList;
//    }

    @Override
    public JSONObject statisticsEvaluateTow(Long id,  Date deDate , Date oldDate , int type) {
        QualityLogDO QualityLogById = QualityLogService.getQualityLogById(id);
        LambdaQueryWrapperX<QualityLogDO> objectLambdaQueryWrapperX = new LambdaQueryWrapperX<>();
        objectLambdaQueryWrapperX.betweenIfPresent(QualityLogDO::getCreateTime , oldDate , deDate);
        objectLambdaQueryWrapperX.eqIfPresent(QualityLogDO::getQualityId , QualityLogById.getQualityId());
        List<QualityLogDO> list = QualityLogService.list(objectLambdaQueryWrapperX);

        LocalDate today = LocalDate.now();
        List<String> lastSevenDays = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        switch (type) {
            case 0:
                // 循环生成最近 7 天的日期
                for (int i = 0; i < 7; i++) {
                    LocalDate date = today.minusDays(i);
                    // 格式化日期并添加到列表中
                    lastSevenDays.add(date.format(formatter));
                }
                break;
            case 1:
                // 循环生成最近 15 天的日期
                for (int i = 0; i < 15; i++) {
                    LocalDate date = today.minusDays(i);
                    // 格式化日期并添加到列表中
                    lastSevenDays.add(date.format(formatter));
                }
                break;
            case 2:
                // 循环生成最近 30 天的日期
                for (int i = 0; i < 30; i++) {
                    LocalDate date = today.minusDays(i);
                    // 格式化日期并添加到列表中
                    lastSevenDays.add(date.format(formatter));
                }
                break;
        }
        Map<String , Integer> map = new HashMap<>();
        for (QualityLogDO aDo : list) {
            String s = DateUtils.parseDateToStr("yyyy-MM-dd", aDo.getCreateTime());
            for (String lastSevenDay : lastSevenDays) {
                if (s.equals(lastSevenDay)) {
                    Integer i = map.get(lastSevenDay);
                    if (i == null) {
                        i = 0;
                    }
                    if (aDo.getScore() != null) {
                        i += Convert.toInt(aDo.getScore());
                    }
                    map.put("lastSevenDay" , i);
                }
            }
        }
        List<Integer> value = new ArrayList<>();
        for (String string : lastSevenDays) {
            Integer i = map.get(string);
            if (i == null) {
                i = 0;
            }
            value.add(i);
        }

        JSONObject json = new JSONObject();
        json.put("title" , lastSevenDays);
        json.put("value" , value);
        return json;
    }

    public List<QualityTaskObjRespVO> buildTaskObjRespList(QualityLogDO QualityLogById) {
        if (QualityLogById == null || QualityLogById.getQualityId() == null) {
            return Collections.emptyList();
        }

        QualityTaskObjPageReqVO reqVO = new QualityTaskObjPageReqVO();
        reqVO.setTaskId(QualityLogById.getQualityId());

        List<QualityTaskObjDO> lists = QualityTaskObjService.getQualityTaskObjList(reqVO);
        List<QualityTaskObjRespVO> result = new ArrayList<>();

        if (CollectionUtils.isNotEmpty(lists)) {
            for (QualityTaskObjDO objDO : lists) {
                QualityTaskObjRespVO bean = BeanUtils.toBean(objDO, QualityTaskObjRespVO.class);

                DatasourceRespDTO ds = daDatasourceApiService.getDatasourceById(objDO.getDatasourceId());
                if (ds != null) {
                    bean.setDatasourceType(ds.getDatasourceType());
                    bean.setDatasourceName(ds.getDatasourceName());
                }
                result.add(bean);
            }
        }
        return result;
    }

    @Override
    public List<EvaluateLogRespVO> statisticsEvaluateTable(Long id) {
        QualityLogDO QualityLogById = QualityLogService.getQualityLogById(id);
        if(QualityLogById == null){
            return new ArrayList<>();
        }
        List<EvaluateLogDO> taskLogId = EvaluateLogMapper.selectList("task_log_id", String.valueOf(id));

        List<QualityTaskObjRespVO> newList = this.buildTaskObjRespList(QualityLogById);

        List<EvaluateLogRespVO> list = new ArrayList<>();
        Map<Long, QualityTaskObjRespVO> collect = newList.stream().collect(Collectors.toMap(s -> s.getId(), Function.identity()));
        for (EvaluateLogDO EvaluateLogDO : taskLogId) {
            QualityTaskEvaluateDO QualityTaskEvaluateById = QualityTaskEvaluateService.getQualityTaskEvaluateById(Convert.toLong(EvaluateLogDO.getEvaluateId()));
            QualityTaskObjRespVO QualityTaskObjRespVO = collect.get(QualityTaskEvaluateById.getObjId());

            EvaluateLogRespVO bean = BeanUtils.toBean(EvaluateLogDO, EvaluateLogRespVO.class);
            // 旧的执行日志未必保存了字段名和规则配置；用任务中的评测配置回填，
            // 使历史质量报告也能显示被校验的字段。
            if (StringUtils.isBlank(bean.getColumnName())) {
                bean.setColumnName(QualityTaskEvaluateById.getEvaColumn());
            }
            if (StringUtils.isBlank(bean.getRule())) {
                bean.setRule(QualityTaskEvaluateById.getRule());
            }
            if (StringUtils.isBlank(bean.getTableName())) {
                bean.setTableName(QualityTaskEvaluateById.getTableName());
            }
            Long total = bean.getTotal();
            Long problemTotal = bean.getProblemTotal();
            bean.setProportion(BigDecimal.ZERO);
            if (!total.equals(0L)) {
                BigDecimal bigDecimal = new BigDecimal(problemTotal).divide(new BigDecimal(total) , 5, RoundingMode.HALF_UP);
                BigDecimal subtract = bigDecimal.multiply(new BigDecimal(100));
                bean.setProportion(subtract.setScale(2 , RoundingMode.HALF_UP));
            }

//            JSONObject jsonObject = JSONObject.parseObject(bean.getRule());
//            List<JSONObject> list1 = jsonObject.getList("evaColumns", JSONObject.class);
//            if (list1 != null) {
//                for (JSONObject object : list1) {
//                    String string = object.getString("name");
//                    if (bean.getColumnName().equals(string)) {
//                        bean.setColumnName(object.getString("label"));
//                    }
//                }
//            }
            if (QualityTaskObjRespVO != null) {
                bean.setDatasourceName(QualityTaskObjRespVO.getDatasourceName());
                bean.setDatasourceType(QualityTaskObjRespVO.getDatasourceType());
                bean.setDatasourceId(QualityTaskObjRespVO.getDatasourceId());
            }
            list.add(bean);
        }
        return list;
    }

    @Override
    public JSONObject pageErrorData(CheckErrorDataReqDTO checkErrorDataReqDTO) {
        List<HeaderEntity> headers = new ArrayList<>();
        HeaderEntity headerEntity = new HeaderEntity();
        headerEntity.setKey("Content-Type");
        headerEntity.setValue("application/json");
        headers.add(headerEntity);  // 设置请求头
        try {
            String fullUrl = url + "/pageErrorData";

            // 将对象转为 JSON Map 发送 POST 请求（RequestBody）
            Map<String, Object> paramMap = JSONObject.parseObject(JSONObject.toJSONString(checkErrorDataReqDTO), Map.class);

            HttpUtils.ResponseObject responseObject = HttpUtils.sendPost(fullUrl, paramMap, headers);

            System.out.println(responseObject.toString());
            JSONObject json = JSONObject.parseObject(String.valueOf(responseObject.getBody()));
            JSONObject data = json.getJSONObject("data");
            System.out.println(data.toString());
            return data;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean updateErrorData(CheckErrorDataReqDTO checkErrorDataReqDTO) {
        List<HeaderEntity> headers = new ArrayList<>();
        HeaderEntity header = new HeaderEntity();
        header.setKey("Content-Type");
        header.setValue("application/json");
        headers.add(header);

        try {
            String fullUrl = url + "/updateErrorData";
            Map<String, Object> paramMap = JSONObject.parseObject(JSONObject.toJSONString(checkErrorDataReqDTO), Map.class);

            // 发送 POST 请求（带 JSON 请求体）
            HttpUtils.ResponseObject responseObject = HttpUtils.sendPost(fullUrl, paramMap, headers);
            System.out.println("修改响应：" + responseObject);

            JSONObject result = JSONObject.parseObject(String.valueOf(responseObject.getBody()));
            return result.getBoolean("data"); // CommonResult.data 为 true/false
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
