package com.datamaster.module.ontology.service.impl;

import com.alibaba.fastjson2.JSON;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.SecurityUtils;
import com.datamaster.module.ontology.controller.admin.analysis.vo.*;
import com.datamaster.module.ontology.controller.admin.metric.vo.MetricRespVO;
import com.datamaster.module.ontology.dal.dataobject.*;
import com.datamaster.module.ontology.dal.mapper.*;
import com.datamaster.module.ontology.service.IMetricAnalysisService;
import com.datamaster.module.ontology.service.analysis.*;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;
import static com.datamaster.module.ontology.service.metric.MetricRules.require;

@Service
public class MetricAnalysisServiceImpl implements IMetricAnalysisService {
    @Resource private MetricAnalysisMapper analysisMapper;
    @Resource private MetricMapper metricMapper;
    @Resource private MetricVersionMapper versionMapper;
    @Resource private MetricAnalysisAccessGuard access;
    @Resource private MetricQueryPolicy policy;
    @Resource private MetricQuerySourceGuard sourceGuard;
    @Resource private MetricQueryExecutor executor;

    @Override
    public MetricQueryRespVO query(MetricQueryReqVO input) {
        policy.assertEnabled();
        MetricQueryReqVO req = MetricQueryBuilder.normalize(input);
        MetricRespVO snapshot = snapshot(req, null, true);
        OntologyDO ontology = access.ontology(snapshot.getOntologyId());
        MetricQueryBuilder.Plan plan = MetricQueryBuilder.compile(snapshot, req);
        return executor.execute(snapshot, plan, sourceGuard.authorize(snapshot, plan, ontology));
    }

    @Override
    public MetricMultiQueryRespVO multiQuery(MetricMultiQueryReqVO input) {
        policy.assertEnabled();
        require(input != null && input.getQueries() != null && input.getQueries().size() >= 2 && input.getQueries().size() <= 4, "请选择2至4个指标");
        List<MetricQueryReqVO> queries = new ArrayList<>(); List<MetricRespVO> publications = new ArrayList<>();
        List<MetricQueryBuilder.Plan> plans = new ArrayList<>(); List<com.datamaster.common.database.constants.DbQueryProperty> sources = new ArrayList<>();
        for (MetricQueryReqVO item : input.getQueries()) {
            MetricQueryReqVO query = MetricQueryBuilder.normalize(item); queries.add(query);
            publications.add(snapshot(query, null, true));
        }
        MetricMultiQueryRules.comparable(publications, queries, input.getDerivedOperation());
        for (int i = 0; i < publications.size(); i++) {
            MetricRespVO publication = publications.get(i); MetricQueryBuilder.Plan plan = MetricQueryBuilder.compile(publication, queries.get(i));
            if (!"NONE".equals(input.getDerivedOperation())) require(plan.getColumns().stream().anyMatch(column -> "metric_value".equals(column.getKey())
                    && com.datamaster.module.ontology.service.metric.MetricRules.numeric(column.getDataType())), "临时派生要求各指标聚合输出为数值");
            plans.add(plan); sources.add(sourceGuard.authorize(publication, plan, access.ontology(publication.getOntologyId())));
        }
        List<MetricQueryRespVO> results = new ArrayList<>();
        for (int i = 0; i < publications.size(); i++) results.add(executor.execute(publications.get(i), plans.get(i), sources.get(i)));
        return MetricMultiQueryRules.align(results, input.getDerivedOperation());
    }

    @Override
    public MetricComparisonRespVO compare(MetricComparisonReqVO input) {
        policy.assertEnabled();
        MetricQueryReqVO current = MetricQueryBuilder.normalize(input == null ? null : input.getQuery());
        MetricQueryReqVO baseline = MetricComparisonRules.baseline(input);
        MetricRespVO publication = snapshot(current, null, true);
        MetricComparisonRules.validateFilters(publication, current);
        OntologyDO ontology = access.ontology(publication.getOntologyId());
        MetricQueryBuilder.Plan currentPlan = MetricQueryBuilder.compile(publication, current);
        MetricQueryBuilder.Plan baselinePlan = MetricQueryBuilder.compile(publication, baseline);
        require(currentPlan.getColumns().stream().anyMatch(column -> "metric_value".equals(column.getKey())
                && com.datamaster.module.ontology.service.metric.MetricRules.numeric(column.getDataType())), "两期变化计算只支持数值指标");
        // Authorize both before executing either. Both use one exact publication, never current draft definitions.
        com.datamaster.common.database.constants.DbQueryProperty currentSource = sourceGuard.authorize(publication, currentPlan, ontology);
        com.datamaster.common.database.constants.DbQueryProperty baselineSource = sourceGuard.authorize(publication, baselinePlan, ontology);
        MetricComparisonRespVO result = new MetricComparisonRespVO(); result.setMode(input.getMode());
        result.setCurrent(executor.execute(publication, currentPlan, currentSource));
        result.setBaseline(executor.execute(publication, baselinePlan, baselineSource));
        result.setCurrentStartTime(current.getStartTime()); result.setCurrentEndTime(current.getEndTime());
        result.setBaselineStartTime(baseline.getStartTime()); result.setBaselineEndTime(baseline.getEndTime());
        MetricPeriodBucketRules.calculate(result, current, baseline);
        result.setNotice("同一明确发布版本、固定口径与相同维度筛选分别查询两期；只对未截断的完整分组对齐，缺失不补零，分组结果不求和冒充总量。上一期为等长区间，上一月/年为日历平移（月底截到有效日期），实际区间已列出；时间桶只对齐完整、等数量日历桶的序号并显示对应基期桶，不猜测业务日期等价；时间属性额外过滤不支持。百分比=(本期-基期)/|基期|，基期为零或缺失时不计算。两次查询不是源库同一事务快照；时间口径与偏移按同一发布版本执行，不猜测时区或夏令时。");
        return result;
    }

    @Override
    public PageResult<MetricAnalysisRespVO> page(MetricAnalysisPageReqVO req) {
        access.ontology(req.getOntologyId());
        require(req.getPageNum() != null && req.getPageNum() > 0 && req.getPageSize() != null
                && req.getPageSize() >= 1 && req.getPageSize() <= 100, "分页参数无效");
        PageResult<MetricAnalysisDO> page = analysisMapper.selectOwnedPage(req, userId());
        List<MetricAnalysisRespVO> rows = page.getRows().stream().map(this::response).collect(Collectors.toList());
        return new PageResult<>(rows, page.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(MetricAnalysisSaveReqVO req) {
        validateSave(req); require(req.getId() == null, "新增分析不能指定ID");
        access.ontology(req.getOntologyId());
        MetricQueryReqVO query = MetricQueryBuilder.normalize(req.getQuery());
        MetricQueryBuilder.compile(snapshot(query, req.getOntologyId(), false), query);
        MetricAnalysisDO row = new MetricAnalysisDO();
        row.setOntologyId(req.getOntologyId()); row.setOwnerId(userId()); row.setRevision(0);
        apply(row, req, query); analysisMapper.insert(row); return row.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer update(MetricAnalysisSaveReqVO req) {
        validateSave(req); require(req.getId() != null, "更新分析必须指定ID");
        require(req.getRevision() != null && req.getRevision() >= 0, "更新分析必须指定revision");
        MetricAnalysisDO row = owned(req.getId(), true);
        require(Objects.equals(row.getOntologyId(), req.getOntologyId()), "分析所属本体不能改变");
        require(Objects.equals(row.getRevision(), req.getRevision()), "分析已被修改，请刷新后重试（revision不匹配）");
        MetricQueryReqVO query = MetricQueryBuilder.normalize(req.getQuery());
        MetricQueryBuilder.compile(snapshot(query, row.getOntologyId(), false), query);
        // Row lock covers owner/revision check and update until transaction commit.
        apply(row, req, query); row.setRevision(Math.addExact(row.getRevision(), 1));
        return analysisMapper.updateById(row);
    }

    @Override
    public MetricAnalysisRespVO get(Long id) { return response(owned(id, false)); }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer delete(Long id) {
        MetricAnalysisDO row = owned(id, true);
        return analysisMapper.deleteById(row.getId());
    }

    private MetricAnalysisDO owned(Long id, boolean lock) {
        require(id != null, "分析ID不能为空");
        Long ownerId = userId();
        MetricAnalysisDO row = lock ? analysisMapper.selectOwnedForUpdate(id, ownerId)
                : analysisMapper.selectOne(new LambdaQueryWrapperX<MetricAnalysisDO>()
                    .eq(MetricAnalysisDO::getId, id).eq(MetricAnalysisDO::getOwnerId, ownerId));
        require(row != null && Objects.equals(row.getOwnerId(), ownerId), "分析不存在或无权访问");
        access.ontology(row.getOntologyId());
        // Read/delete also verify the saved metric/version belongs to this ontology. No source IO.
        validateStored(row);
        return row;
    }

    private MetricAnalysisRespVO response(MetricAnalysisDO row) {
        require(Objects.equals(row.getOwnerId(), userId()), "分析不存在或无权访问");
        access.ontology(row.getOntologyId());
        MetricQueryReqVO query = validateStored(row);
        MetricAnalysisRespVO result = new MetricAnalysisRespVO(); BeanUtils.copyProperties(row, result);
        result.setQuery(query); return result;
    }

    private MetricQueryReqVO validateStored(MetricAnalysisDO row) {
        MetricQueryReqVO query;
        try { query = JSON.parseObject(row.getQueryJson(), MetricQueryReqVO.class); }
        catch (RuntimeException ex) { throw new ServiceException("已保存分析定义不可用"); }
        query = MetricQueryBuilder.normalize(query);
        require(Objects.equals(row.getMetricId(), query.getMetricId()) && Objects.equals(row.getVersion(), query.getVersion()),
                "已保存分析版本身份不一致");
        snapshot(query, row.getOntologyId(), false);
        return query;
    }

    private MetricRespVO snapshot(MetricQueryReqVO query, Long ontologyId, boolean executable) {
        MetricDO metric = metricMapper.selectById(query.getMetricId());
        require(metric != null, "指标不存在"); access.ontology(metric.getOntologyId());
        if (ontologyId != null) require(Objects.equals(ontologyId, metric.getOntologyId()), "指标不属于该分析本体");
        if (executable) require("PUBLISHED".equals(metric.getStatus()), "指标尚未发布或已停用，禁止执行");
        MetricVersionDO version = versionMapper.selectOne(new LambdaQueryWrapperX<MetricVersionDO>()
                .eq(MetricVersionDO::getMetricId, query.getMetricId()).eq(MetricVersionDO::getVersion, query.getVersion()));
        require(version != null, "指定指标发布版本不存在");
        MetricRespVO snapshot;
        try { snapshot = JSON.parseObject(version.getSnapshotJson(), MetricRespVO.class); }
        catch (RuntimeException ex) { throw new ServiceException("指标发布快照不可用"); }
        MetricQueryBuilder.checkIdentity(metric, snapshot, query.getMetricId(), query.getVersion());
        return snapshot;
    }
    private static void validateSave(MetricAnalysisSaveReqVO req) {
        require(req != null && req.getOntologyId() != null, "本体ID不能为空");
        require(req.getName() != null && !req.getName().trim().isEmpty() && req.getName().length() <= 100, "分析名称必填且最多100字符");
        require(req.getDescription() == null || req.getDescription().length() <= 1000, "分析说明最多1000字符");
        require(req.getQuery() != null, "分析查询定义不能为空");
    }
    private static Long userId() { Long id = SecurityUtils.getUserId(); require(id != null, "用户未登录"); return id; }
    private static void apply(MetricAnalysisDO row, MetricAnalysisSaveReqVO req, MetricQueryReqVO query) {
        row.setName(req.getName()); row.setDescription(req.getDescription());
        row.setMetricId(query.getMetricId()); row.setVersion(query.getVersion());
        row.setQueryJson(JSON.toJSONString(query));
    }
}
