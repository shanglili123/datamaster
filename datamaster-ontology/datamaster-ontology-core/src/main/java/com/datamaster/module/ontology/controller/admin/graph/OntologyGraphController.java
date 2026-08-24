package com.datamaster.module.ontology.controller.admin.graph;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.datamaster.common.core.domain.CommonResult;
import com.datamaster.module.ontology.dal.dataobject.*;
import com.datamaster.module.ontology.dal.mapper.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.*;

@Tag(name = "本体图谱可视化")
@RestController
@RequestMapping("/ont/graph")
public class OntologyGraphController {

    @Resource private ConceptMapper conceptMapper;
    @Resource private PropertyMapper propertyMapper;
    @Resource private RelationMapper relationMapper;
    @Resource private ConceptTableMapper conceptTableMapper;

    @Operation(summary = "获取本体图谱数据（nodes + edges）")
    @PreAuthorize("@ss.hasPermi('ont:concept:list')")
    @GetMapping("/data")
    public CommonResult<Map<String, Object>> getGraphData(@RequestParam Long ontologyId) {
        List<ConceptDO> concepts = conceptMapper.selectList(
                new LambdaQueryWrapper<ConceptDO>().eq(ConceptDO::getOntologyId, ontologyId));
        List<RelationDO> relations = relationMapper.selectList(
                new LambdaQueryWrapper<RelationDO>().eq(RelationDO::getOntologyId, ontologyId));

        // Build nodes
        List<Map<String, Object>> nodes = new ArrayList<>();
        for (ConceptDO concept : concepts) {
            List<PropertyDO> props = propertyMapper.selectList(
                    new LambdaQueryWrapper<PropertyDO>().eq(PropertyDO::getConceptId, concept.getId()));
            List<String> propLabels = new ArrayList<>();
            for (PropertyDO p : props) {
                propLabels.add(p.getName() + " : " + (p.getDataType() != null ? p.getDataType() : "string"));
            }
            List<ConceptTableDO> binds = conceptTableMapper.selectList(
                    new LambdaQueryWrapper<ConceptTableDO>().eq(ConceptTableDO::getConceptId, concept.getId()));
            List<String> tableNames = new ArrayList<>();
            for (ConceptTableDO ct : binds) {
                tableNames.add(ct.getTableName());
            }
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("id", "concept_" + concept.getId());
            node.put("label", concept.getName());
            node.put("type", "concept");
            node.put("conceptId", concept.getId());
            node.put("code", concept.getCode());
            node.put("description", concept.getDescription());
            node.put("color", concept.getColor());
            node.put("boundTables", tableNames);
            node.put("properties", propLabels);
            nodes.add(node);
        }

        // Build edges
        List<Map<String, Object>> edges = new ArrayList<>();
        for (RelationDO rel : relations) {
            Map<String, Object> edge = new LinkedHashMap<>();
            edge.put("id", "relation_" + rel.getId());
            edge.put("source", "concept_" + rel.getSourceConceptId());
            edge.put("target", "concept_" + rel.getTargetConceptId());
            edge.put("label", rel.getName());
            edge.put("type", rel.getRelationType());
            edge.put("relationId", rel.getId());
            edges.add(edge);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("nodes", nodes);
        result.put("edges", edges);
        return CommonResult.success(result);
    }
}
