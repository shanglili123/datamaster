package com.datamaster.module.ai.controller.agent;

import com.alibaba.fastjson2.JSON;
import com.datamaster.common.core.domain.CommonResult;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.SecurityUtils;
import com.datamaster.module.ai.agent.decision.DecisionModels.*;
import com.datamaster.module.ai.agent.decision.DecisionProposalService;
import com.datamaster.module.ontology.controller.admin.analysis.vo.MetricQueryReqVO;
import com.datamaster.module.ontology.controller.admin.analysis.vo.MetricQueryRespVO;
import com.datamaster.module.ontology.dal.dataobject.MetricDO;
import com.datamaster.module.ontology.dal.mapper.MetricMapper;
import com.datamaster.module.ontology.service.IMetricAnalysisService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.*;

/** Replays only server-recorded read queries; never invokes a model or modifies a plan. */
@RestController
@RequestMapping("/ai/decision")
public class DecisionReviewController {
    @Resource private DecisionProposalService proposals;
    @Resource private IMetricAnalysisService analysis;
    @Resource private MetricMapper metricMapper;

    @PostMapping("/{id}/review")
    @PreAuthorize("@ss.hasPermi('ai:ask-data:query') and @ss.hasPermi('ont:metric:query') and @ss.hasPermi('ont:metric:execute')")
    public CommonResult<Proposal> review(@PathVariable String id) {
        if (!SecurityUtils.hasPermi("ont:metric:query") || !SecurityUtils.hasPermi("ont:metric:execute"))
            throw new ServiceException("缺少指标复查权限");
        Proposal proposal = proposals.refresh(id);
        if (!"SUCCEEDED".equals(proposal.getStatus())) throw new ServiceException("业务执行尚未成功，不能进行执行后效果复查");
        List<Map<String, Object>> observations = new ArrayList<>();
        for (Evidence evidence : proposal.getEvidence()) {
            if (!"VERIFIED_METRIC".equals(evidence.getVerification())) continue;
            Object raw = evidence.getDetails().get("queryEvidence");
            if (!(raw instanceof List)) continue;
            for (Object entry : (List<?>) raw) {
                Map<?, ?> tool = JSON.parseObject(JSON.toJSONString(entry), Map.class);
                if (!"execute_metric_analysis".equals(tool.get("toolName"))) continue;
                Object data = tool.get("data");
                if (!(data instanceof Map)) continue;
                Object definition = ((Map<?, ?>) data).get("queryDefinition");
                if (definition == null) continue;
                MetricQueryReqVO query = JSON.parseObject(JSON.toJSONString(definition), MetricQueryReqVO.class);
                MetricDO metric = metricMapper.selectById(query.getMetricId());
                if (metric == null || !String.valueOf(metric.getOntologyId()).equals(proposal.getOntologyId()))
                    throw new ServiceException("复查指标不属于当前提案本体");
                MetricQueryRespVO result = analysis.query(query);
                Map<String, Object> observation = new LinkedHashMap<>();
                observation.put("metricId", String.valueOf(query.getMetricId())); observation.put("version", query.getVersion());
                observation.put("executedAt", result.getExecutedAt()); observation.put("truncated", result.getTruncated());
                observation.put("rows", result.getRows() == null ? Collections.emptyList()
                        : result.getRows().subList(0, Math.min(result.getRows().size(), 3)));
                observations.add(observation);
                if (observations.size() >= 3) break;
            }
            if (observations.size() >= 3) break;
        }
        String message = observations.isEmpty()
                ? "没有可重放的正式单指标查询证据；探索回复/两期比较不能冒充同口径执行后复查，业务效果未知"
                : "已按原版本、原日期与过滤进行只读复查（不自动改变统计窗口）。非源数据刷新证明，不做因果归因，目标改善仍待业务验证。观测：" + JSON.toJSONString(observations);
        return CommonResult.success(proposals.updateEffect(id, proposal.getRevision(),
                observations.isEmpty() ? "UNKNOWN" : "INCONCLUSIVE", message));
    }
}
