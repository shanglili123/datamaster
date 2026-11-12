package com.datamaster.module.ontology.service.analysis;

import com.datamaster.module.ontology.dal.dataobject.ConceptDO;
import com.datamaster.module.ontology.dal.dataobject.OntologyDO;
import org.springframework.stereotype.Component;
import javax.annotation.Resource;
import java.util.Objects;
import static com.datamaster.module.ontology.service.metric.MetricRules.require;

/** Canonical space from the stored concept's ontology, never from a browser-supplied policy selector. */
@Component
public class OntologyConsumptionContext {
    @Resource private MetricAnalysisAccessGuard access;
    public OntologyDO resolve(ConceptDO concept, Long requestedOntology, Long requestedSpace, String requestedCode) {
        require(concept != null && concept.getOntologyId() != null, "概念所属本体不能为空");
        require(requestedOntology == null || Objects.equals(requestedOntology, concept.getOntologyId()), "请求本体与概念归属不一致");
        OntologyDO ontology = access.ontology(concept.getOntologyId());
        require(requestedSpace == null || Objects.equals(requestedSpace, ontology.getSpaceId()), "请求空间与本体归属不一致");
        require(requestedCode == null || requestedCode.isBlank() || Objects.equals(requestedCode, ontology.getSpaceCode()), "请求空间编码与本体归属不一致");
        return ontology;
    }
}
