

package com.datamaster.module.collector.service.etl.impl;

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
import com.datamaster.module.assets.api.datasource.dto.AssetsDatasourceRespDTO;
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
import com.datamaster.common.utils.JSONUtils;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.common.utils.object.BeanUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.datamaster.module.assets.api.service.asset.IAssetsDatasourceApiService;
import com.datamaster.module.collector.controller.admin.etl.vo.*;
import com.datamaster.module.collector.controller.admin.qa.vo.*;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEvaluateLogDO;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorQualityLogDO;
import com.datamaster.module.collector.dal.dataobject.qa.CollectorQualityTaskEvaluateDO;
import com.datamaster.module.collector.dal.dataobject.qa.CollectorQualityTaskObjDO;
import com.datamaster.module.collector.dal.mapper.etl.CollectorEvaluateLogMapper;
import com.datamaster.module.collector.service.etl.ICollectorEvaluateLogService;
import com.datamaster.module.collector.service.etl.ICollectorQualityLogService;
import com.datamaster.module.collector.service.qa.ICollectorQualityTaskEvaluateService;
import com.datamaster.module.collector.service.qa.ICollectorQualityTaskObjService;
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
public class CollectorEvaluateLogServiceImpl  extends ServiceImpl<CollectorEvaluateLogMapper,CollectorEvaluateLogDO> implements ICollectorEvaluateLogService {

    @Value("${path.quality_url}")
    private String url;
    @Resource
    private CollectorEvaluateLogMapper CollectorEvaluateLogMapper;

    @Resource
    @Lazy
    private ICollectorQualityLogService CollectorQualityLogService;
    @Resource
    @Lazy
    private ICollectorQualityTaskEvaluateService CollectorQualityTaskEvaluateService;
    @Resource
    @Lazy
    private ICollectorQualityTaskObjService CollectorQualityTaskObjService;
    @Resource
    @Lazy
    private IAssetsDatasourceApiService daDatasourceApiService;

    @Override
    public PageResult<CollectorEvaluateLogDO> getCollectorEvaluateLogPage(CollectorEvaluateLogPageReqVO pageReqVO) {
        return CollectorEvaluateLogMapper.selectPage(pageReqVO);
    }

    @Override
    public Long createCollectorEvaluateLog(CollectorEvaluateLogSaveReqVO createReqVO) {
        CollectorEvaluateLogDO dictType = BeanUtils.toBean(createReqVO, CollectorEvaluateLogDO.class);
        CollectorEvaluateLogMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public int updateCollectorEvaluateLog(CollectorEvaluateLogSaveReqVO updateReqVO) {
        // 相关校验

        // 更新评测规则结果
        CollectorEvaluateLogDO updateObj = BeanUtils.toBean(updateReqVO, CollectorEvaluateLogDO.class);
        return CollectorEvaluateLogMapper.updateById(updateObj);
    }
    @Override
    public int removeCollectorEvaluateLog(Collection<Long> idList) {
        // 批量删除评测规则结果
        return CollectorEvaluateLogMapper.deleteBatchIds(idList);
    }

    @Override
    public CollectorEvaluateLogDO getCollectorEvaluateLogById(Long id) {
        return CollectorEvaluateLogMapper.selectById(id);
    }

    @Override
    public List<CollectorEvaluateLogDO> getCollectorEvaluateLogList() {
        return CollectorEvaluateLogMapper.selectList();
    }

    @Override
    public Map<Long, CollectorEvaluateLogDO> getCollectorEvaluateLogMap() {
        List<CollectorEvaluateLogDO> CollectorEvaluateLogList = CollectorEvaluateLogMapper.selectList();
        return CollectorEvaluateLogList.stream()
                .collect(Collectors.toMap(
                        CollectorEvaluateLogDO::getId,
                        CollectorEvaluateLogDO -> CollectorEvaluateLogDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing
                ));
    }

    @Override
    public Map<String, Object> sumTotalAndProblemTotalByTaskLogId(String taskLogId) {
        List<CollectorEvaluateLogDO> list = CollectorEvaluateLogMapper.selectList(new LambdaQueryWrapperX<CollectorEvaluateLogDO>()
                .eq(CollectorEvaluateLogDO::getTaskLogId, taskLogId)
                .eq(CollectorEvaluateLogDO::getValidFlag, "1")); // 如有需要加条件

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
    public String importCollectorEvaluateLog(List<CollectorEvaluateLogRespVO> importExcelList, boolean isUpdateSupport, String operName) {
        if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
            throw new ServiceException("导入数据不能为空！");
        }

        int successNum = 0;
        int failureNum = 0;
        List<String> successMessages = new ArrayList<>();
        List<String> failureMessages = new ArrayList<>();

        for (CollectorEvaluateLogRespVO respVO : importExcelList) {
            try {
                CollectorEvaluateLogDO CollectorEvaluateLogDO = BeanUtils.toBean(respVO, CollectorEvaluateLogDO.class);
                Long CollectorEvaluateLogId = respVO.getId();
                if (isUpdateSupport) {
                    if (CollectorEvaluateLogId != null) {
                        CollectorEvaluateLogDO existingCollectorEvaluateLog = CollectorEvaluateLogMapper.selectById(CollectorEvaluateLogId);
                        if (existingCollectorEvaluateLog != null) {
                            CollectorEvaluateLogMapper.updateById(CollectorEvaluateLogDO);
                            successNum++;
                            successMessages.add("数据更新成功，ID为 " + CollectorEvaluateLogId + " 的评测规则结果记录。");
                        } else {
                            failureNum++;
                            failureMessages.add("数据更新失败，ID为 " + CollectorEvaluateLogId + " 的评测规则结果记录不存在。");
                        }
                    } else {
                        failureNum++;
                        failureMessages.add("数据更新失败，某条记录的ID不存在。");
                    }
                } else {
                    QueryWrapper<CollectorEvaluateLogDO> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("id", CollectorEvaluateLogId);
                    CollectorEvaluateLogDO existingCollectorEvaluateLog = CollectorEvaluateLogMapper.selectOne(queryWrapper);
                    if (existingCollectorEvaluateLog == null) {
                        CollectorEvaluateLogMapper.insert(CollectorEvaluateLogDO);
                        successNum++;
                        successMessages.add("数据插入成功，ID为 " + CollectorEvaluateLogId + " 的评测规则结果记录。");
                    } else {
                        failureNum++;
                        failureMessages.add("数据插入失败，ID为 " + CollectorEvaluateLogId + " 的评测规则结果记录已存在。");
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
    public List<CollectorEvaluateLogStatisticsVO> statisticsEvaluateOne(Long id) {
        List<CollectorEvaluateLogStatisticsVO> CollectorEvaluateDimStatVOS = CollectorEvaluateLogMapper.selectDimStatsByTaskLogId(id);
        if(CollectorEvaluateDimStatVOS.isEmpty()){
            return new ArrayList<>();
        }
        CollectorQualityLogDO CollectorQualityLogDO = CollectorQualityLogService.selectPrevLogByIdWithWrapper(id);
        if(CollectorQualityLogDO == null){
            for (CollectorEvaluateLogStatisticsVO vo : CollectorEvaluateDimStatVOS) {
                vo.setTrendType(3L);
            }
            return CollectorEvaluateDimStatVOS;
        }
        List<CollectorEvaluateLogStatisticsVO> prevList = CollectorEvaluateLogMapper.selectDimStatsByTaskLogId(CollectorQualityLogDO.getId());
        if(prevList == null || prevList.isEmpty()){
            for (CollectorEvaluateLogStatisticsVO vo : CollectorEvaluateDimStatVOS) {
                vo.setTrendType(3L);
            }
            return CollectorEvaluateDimStatVOS;
        }

        // 4) 以维度为 Key 的上次“问题占比”基线
        Map<String, BigDecimal> prevProportionMap = new HashMap<>(prevList.size() * 2);
        for (CollectorEvaluateLogStatisticsVO vo : prevList) {
            BigDecimal val = vo.getProportion() == null ? BigDecimal.ZERO : vo.getProportion();
            prevProportionMap.put(String.valueOf(vo.getDimensionType()), val);
        }

        for (CollectorEvaluateLogStatisticsVO vo : CollectorEvaluateDimStatVOS) {
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
//    public List<CollectorEvaluateLogStatisticsVO> statisticsEvaluateOne(Long id) {
//            List<CollectorEvaluateLogStatisticsVO> voList = new ArrayList<>();
//        CollectorQualityLogDO CollectorQualityLogById = CollectorQualityLogService.getCollectorQualityLogById(id);
//        CollectorQualityLogPageReqVO CollectorQualityLogPageReqVO = new CollectorQualityLogPageReqVO();
//        CollectorQualityLogPageReqVO.setQualityId(CollectorQualityLogById.getQualityId());
//        PageResult<CollectorQualityLogDO> CollectorQualityLogPage = CollectorQualityLogService.getCollectorQualityLogPage(CollectorQualityLogPageReqVO);
//        List<CollectorQualityLogDO> rows =  (List<CollectorQualityLogDO>) CollectorQualityLogPage.getRows();
//        CollectorQualityLogPageReqVO old = new CollectorQualityLogPageReqVO();
//        for (CollectorQualityLogDO row : rows) {
//            if (row.getCreateTime().getTime() <= CollectorQualityLogById.getCreateTime().getTime()) {
//                old = BeanUtils.toBean(row , CollectorQualityLogPageReqVO.class);
//                break;
//            }
//        }
//        // 最新的
//        Long problemTotalAll = 0L;
//        List<CollectorEvaluateLogDO> taskLogId = CollectorEvaluateLogMapper.selectList("task_log_id", id);
//        Map<String, List<CollectorEvaluateLogDO>> collect = taskLogId.stream().collect(Collectors.groupingBy(s -> s.getDimensionType()));
//        for (CollectorEvaluateLogDO aDo : taskLogId) {
//            problemTotalAll += aDo.getProblemTotal();
//        }
//
//        // 老的
//        List<CollectorEvaluateLogDO> oldTaskLogId = CollectorEvaluateLogMapper.selectList("task_log_id", old.getId());
//        Map<String, List<CollectorEvaluateLogDO>> oldCollect = oldTaskLogId.stream().collect(Collectors.groupingBy(s -> s.getDimensionType()));
//        for (CollectorEvaluateLogDO aDo : oldTaskLogId) {
//
//        }
//        for (Map.Entry<String, List<CollectorEvaluateLogDO>> entry : collect.entrySet()) {
//            CollectorEvaluateLogStatisticsVO vo = new CollectorEvaluateLogStatisticsVO();
//            List<CollectorEvaluateLogDO> value = entry.getValue();
//            Long problemTotal = 0L;
//            for (CollectorEvaluateLogDO CollectorEvaluateLogDO : value) {
//                problemTotal += CollectorEvaluateLogDO.getProblemTotal();
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
//            List<CollectorEvaluateLogDO> CollectorEvaluateLogDOS = oldCollect.get(entry.getKey());
//            Long oldProblemTotal = 0L;
//            if (CollectorEvaluateLogDOS != null) {
//                for (CollectorEvaluateLogDO CollectorEvaluateLogDO : CollectorEvaluateLogDOS) {
//                    oldProblemTotal += CollectorEvaluateLogDO.getProblemTotal();
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
        CollectorQualityLogDO CollectorQualityLogById = CollectorQualityLogService.getCollectorQualityLogById(id);
        LambdaQueryWrapperX<CollectorQualityLogDO> objectLambdaQueryWrapperX = new LambdaQueryWrapperX<>();
        objectLambdaQueryWrapperX.betweenIfPresent(CollectorQualityLogDO::getCreateTime , oldDate , deDate);
        objectLambdaQueryWrapperX.eqIfPresent(CollectorQualityLogDO::getQualityId , CollectorQualityLogById.getQualityId());
        List<CollectorQualityLogDO> list = CollectorQualityLogService.list(objectLambdaQueryWrapperX);

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
        for (CollectorQualityLogDO aDo : list) {
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

    public List<CollectorQualityTaskObjRespVO> buildTaskObjRespList(CollectorQualityLogDO CollectorQualityLogById) {
        if (CollectorQualityLogById == null || CollectorQualityLogById.getQualityId() == null) {
            return Collections.emptyList();
        }

        CollectorQualityTaskObjPageReqVO reqVO = new CollectorQualityTaskObjPageReqVO();
        reqVO.setTaskId(JSONUtils.convertToLong(CollectorQualityLogById.getQualityId()));

        List<CollectorQualityTaskObjDO> lists = CollectorQualityTaskObjService.getCollectorQualityTaskObjList(reqVO);
        List<CollectorQualityTaskObjRespVO> result = new ArrayList<>();

        if (CollectionUtils.isNotEmpty(lists)) {
            for (CollectorQualityTaskObjDO objDO : lists) {
                CollectorQualityTaskObjRespVO bean = BeanUtils.toBean(objDO, CollectorQualityTaskObjRespVO.class);

                AssetsDatasourceRespDTO ds = daDatasourceApiService.getDatasourceById(objDO.getDatasourceId());
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
    public List<CollectorEvaluateLogRespVO> statisticsEvaluateTable(Long id) {
        CollectorQualityLogDO CollectorQualityLogById = CollectorQualityLogService.getCollectorQualityLogById(id);
        if(CollectorQualityLogById == null){
            return new ArrayList<>();
        }
        List<CollectorEvaluateLogDO> taskLogId = CollectorEvaluateLogMapper.selectList("task_log_id", id);

        List<CollectorQualityTaskObjRespVO> newList = this.buildTaskObjRespList(CollectorQualityLogById);

        List<CollectorEvaluateLogRespVO> list = new ArrayList<>();
        Map<Long, CollectorQualityTaskObjRespVO> collect = newList.stream().collect(Collectors.toMap(s -> s.getId(), Function.identity()));
        for (CollectorEvaluateLogDO CollectorEvaluateLogDO : taskLogId) {
            CollectorQualityTaskEvaluateDO CollectorQualityTaskEvaluateById = CollectorQualityTaskEvaluateService.getCollectorQualityTaskEvaluateById(Convert.toLong(CollectorEvaluateLogDO.getEvaluateId()));
            CollectorQualityTaskObjRespVO CollectorQualityTaskObjRespVO = collect.get(CollectorQualityTaskEvaluateById.getObjId());

            CollectorEvaluateLogRespVO bean = BeanUtils.toBean(CollectorEvaluateLogDO, CollectorEvaluateLogRespVO.class);
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
            if (CollectorQualityTaskObjRespVO != null) {
                bean.setDatasourceName(CollectorQualityTaskObjRespVO.getDatasourceName());
                bean.setDatasourceType(CollectorQualityTaskObjRespVO.getDatasourceType());
                bean.setDatasourceId(CollectorQualityTaskObjRespVO.getDatasourceId());
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
