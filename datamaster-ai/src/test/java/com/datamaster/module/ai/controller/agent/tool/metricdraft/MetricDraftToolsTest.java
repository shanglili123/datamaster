package com.datamaster.module.ai.controller.agent.tool.metricdraft;

import com.alibaba.fastjson2.JSON;
import com.datamaster.common.datasource.mgmt.api.IDatasourceApiService;
import com.datamaster.common.datasource.mgmt.api.dto.DatasourceRespDTO;
import com.datamaster.module.ai.agent.runtime.AgentPayloadSanitizer;
import com.datamaster.module.ai.agent.runtime.AgentRunResponse;
import com.datamaster.module.ai.agent.tool.*;
import com.datamaster.module.ai.controller.agent.tool.MetricAgentToolSupport;
import com.datamaster.module.ai.dal.dataobject.agent.AiAgentOperationDO;
import com.datamaster.module.ai.service.agent.IAiAgentOperationService;
import com.datamaster.module.ontology.dal.dataobject.*;
import com.datamaster.module.ontology.dal.mapper.*;
import dev.langchain4j.agent.tool.ToolSpecifications;
import org.junit.Test;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.*;
import static org.junit.Assert.*;

/** Focused schema, real-mapping and confirmation regression cases. Intentionally not executed by this change. */
public class MetricDraftToolsTest {
    @Test
    public void toolsHaveConcreteSchemasAndStringBusinessIds() throws Exception {
        assertEquals("preview_metrics", ToolSpecifications.toolSpecificationFrom(
                PreviewMetricsTool.class.getMethod("execute", MetricDraftToolRequest.Preview.class)).name());
        assertEquals("save_metric_draft", ToolSpecifications.toolSpecificationFrom(
                SaveMetricDraftTool.class.getMethod("execute", MetricDraftToolRequest.Save.class)).name());
        assertEquals(String.class, MetricDraftToolRequest.Preview.class.getDeclaredField("ontologyId").getType());
        assertEquals(String.class, MetricDraftToolRequest.Candidate.class.getDeclaredField("conceptTableId").getType());
        assertEquals(String.class, MetricDraftToolRequest.Candidate.class.getDeclaredField("valuePropertyId").getType());
        assertEquals(Integer.class, MetricDraftToolRequest.Save.class.getDeclaredField("previewIndex").getType());
        assertFalse(SaveMetricDraftTool.class.getAnnotation(AgentTool.class).requiresConfirmation());
        assertEquals(AgentToolRisk.READ_ONLY, PreviewMetricsTool.class.getAnnotation(AgentTool.class).risk());
        // Client cannot submit an arbitrary definition through the write schema.
        assertEquals(3, MetricDraftToolRequest.Save.class.getDeclaredFields().length);
    }

    @Test
    public void contentDigestSurvivesNormalAuditAndNormalizesObjectKeyOrder() {
        Map<String, Object> a = new LinkedHashMap<>();
        a.put("ontologyId", "9007199254740993123"); a.put("schemaVersion", MetricDraftSupport.SCHEMA);
        Map<String, Object> b = new LinkedHashMap<>();
        b.put("schemaVersion", MetricDraftSupport.SCHEMA); b.put("ontologyId", "9007199254740993123");
        assertEquals(MetricDraftConfirmation.hash(a), MetricDraftConfirmation.hash(b));
        String digest = MetricDraftConfirmation.hash(a);
        a.put("previewDigest", digest);
        Map<?, ?> persisted = JSON.parseObject(AgentPayloadSanitizer.sanitizeJson(a));
        assertEquals(digest, persisted.get("previewDigest"));
        a.put("ontologyId", "9007199254740993124");
        assertNotEquals(digest, MetricDraftConfirmation.hash(a));
    }

    @Test
    public void confirmationOnlyAcceptsLastPendingToolAndOperationStatus() {
        AgentToolResult pending = pending("a");
        String json = state(pending);
        assertNotNull(MetricDraftConfirmation.pending(AgentToolResult.NEED_CONFIRMATION, json));
        assertNull(MetricDraftConfirmation.pending(AgentToolResult.RUNNING, json));
        AgentToolResult next = AgentToolResult.of(AgentToolResult.SUCCEEDED, null, "", "operation");
        next.setToolName("preview_metrics");
        assertNull(MetricDraftConfirmation.pending(AgentToolResult.NEED_CONFIRMATION, state(pending, next)));
        next.setToolName("save_analysis"); next.setStatus(AgentToolResult.NEED_CONFIRMATION);
        assertNull(MetricDraftConfirmation.pending(AgentToolResult.NEED_CONFIRMATION, state(pending, next)));
    }

    @Test
    public void consumedHistoryDefeatsAnOldPendingRecordReintroducedByMerge() {
        AgentToolResult consumed = pending("a");
        consumed.getMetadata().put("confirmationConsumed", true);
        assertNull(MetricDraftConfirmation.pending(AgentToolResult.NEED_CONFIRMATION, state(consumed, pending("a"))));
        assertNotNull(MetricDraftConfirmation.pending(AgentToolResult.NEED_CONFIRMATION, state(consumed, pending("b"))));
    }

    @Test
    public void deterministicArgumentsPreserveIndexAndDigest() {
        MetricDraftToolRequest.Save arguments = MetricDraftConfirmation.arguments(pending("a"));
        assertEquals(Integer.valueOf(2), arguments.getPreviewIndex());
        assertEquals("operation", arguments.getPreviewOperationId());
        assertEquals(repeat("d"), arguments.getPreviewDigest());
    }

    @Test
    public void fallbackOnlyUsesMappedNumericFieldsAndRealTemporalProperty() throws Exception {
        Fixture fixture = new Fixture();
        List<Map<String, Object>> rows = fixture.drafts.preview(fixture.request(), fixture.context);
        assertEquals(2, rows.size());
        Map<?, ?> count = (Map<?, ?>) rows.get(0).get("definition");
        Map<?, ?> sum = (Map<?, ?>) rows.get(1).get("definition");
        assertEquals("COUNT", count.get("aggregation")); assertNull(count.get("valuePropertyId"));
        assertEquals("SUM", sum.get("aggregation")); assertEquals("11", sum.get("valuePropertyId"));
        assertEquals("12", sum.get("timePropertyId")); assertEquals("unknown", sum.get("grain"));
        assertNull(sum.get("owner")); assertNull(sum.get("unit"));
        assertTrue(String.valueOf(sum.get("description")).contains("候选口径待业务确认"));
        assertTrue(rows.get(1).get("candidateHash").toString().matches("[0-9a-f]{64}"));
        assertFalse(JSON.toJSONString(rows).contains("password"));
    }

    @Test
    public void noDorisAdsBindingNeedsInputRatherThanCreatingAnything() throws Exception {
        Fixture fixture = new Fixture();
        fixture.table.setTableName("dwd_orders");
        expectInvalid(() -> fixture.drafts.preview(fixture.request(), fixture.context));
        fixture.table.setTableName("ads_orders"); fixture.datasource.setDatasourceType("MYSQL");
        expectInvalid(() -> fixture.drafts.preview(fixture.request(), fixture.context));
    }

    @Test
    public void rejectsInventedPropertyNonNumericValueAndCrossConceptMappings() throws Exception {
        Fixture fixture = new Fixture();
        MetricDraftToolRequest.Candidate candidate = fixture.candidate();
        candidate.setValuePropertyId("999");
        expectInvalid(() -> fixture.drafts.validated("1", "2", candidate, fixture.context));
        candidate.setValuePropertyId("12");
        expectInvalid(() -> fixture.drafts.validated("1", "2", candidate, fixture.context));
        candidate.setValuePropertyId("11"); fixture.number.setConceptId(99L);
        expectInvalid(() -> fixture.drafts.validated("1", "2", candidate, fixture.context));
    }

    @Test
    public void rejectsDuplicateMappingWrongTimeAndChangedOwnerSpace() throws Exception {
        Fixture fixture = new Fixture();
        MetricDraftToolRequest.Candidate candidate = fixture.candidate();
        candidate.setTimePropertyId("11");
        expectInvalid(() -> fixture.drafts.validated("1", "2", candidate, fixture.context));
        candidate.setTimePropertyId("12"); fixture.columns.add(fixture.columns.get(0));
        expectInvalid(() -> fixture.drafts.validated("1", "2", candidate, fixture.context));
        fixture.columns.remove(fixture.columns.size() - 1);
        fixture.operation.setCreatorId(99L);
        expectInvalid(() -> fixture.drafts.operation(fixture.context));
        fixture.operation.setCreatorId(3L); fixture.operation.setSpaceCode("elsewhere");
        expectInvalid(() -> fixture.drafts.operation(fixture.context));
    }

    @Test
    public void businessOrMappingChangesAlterServerCandidateFingerprint() throws Exception {
        Fixture fixture = new Fixture();
        MetricDraftToolRequest.Candidate candidate = fixture.candidate();
        Object original = fixture.drafts.validated("1", "2", candidate, fixture.context).get("candidateHash");
        candidate.setName("另一个名称");
        assertNotEquals(original, fixture.drafts.validated("1", "2", candidate, fixture.context).get("candidateHash"));
        candidate.setName("总额候选"); fixture.columns.get(0).setColumnName("new_real_amount");
        assertNotEquals(original, fixture.drafts.validated("1", "2", candidate, fixture.context).get("candidateHash"));
    }

    private static AgentToolResult pending(String marker) {
        AgentToolResult result = AgentToolResult.of(AgentToolResult.NEED_CONFIRMATION, null, "", "operation");
        result.setToolName(MetricDraftConfirmation.TOOL);
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("toolName", MetricDraftConfirmation.TOOL); metadata.put("confirmationHash", repeat(marker));
        MetricDraftToolRequest.Save args = new MetricDraftToolRequest.Save();
        args.setPreviewOperationId("operation"); args.setPreviewIndex(2); args.setPreviewDigest(repeat("d"));
        metadata.put("arguments", args); result.setMetadata(metadata); return result;
    }

    private static String repeat(String value) { return String.join("", Collections.nCopies(64, value)); }
    private static String state(AgentToolResult... results) {
        AgentRunResponse state = new AgentRunResponse(); state.setToolResults(Arrays.asList(results));
        return JSON.toJSONString(state);
    }
    private static void expectInvalid(Runnable action) {
        try { action.run(); fail("Expected metadata/context rejection"); }
        catch (IllegalArgumentException expected) { assertNotNull(expected.getMessage()); }
    }
    private static void inject(Object target, String field, Object value) throws Exception {
        Field declared = target.getClass().getDeclaredField(field); declared.setAccessible(true); declared.set(target, value);
    }
    private interface Answer { Object answer(String name, Object[] arguments); }
    @SuppressWarnings("unchecked")
    private static <T> T stub(Class<T> type, Answer answer) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> answer.answer(method.getName(), args));
    }

    private static final class Fixture {
        final MetricDraftSupport drafts = new MetricDraftSupport();
        final AgentToolContext context = new AgentToolContext("operation", 3L, "owner", 4L, "space", Collections.emptyMap());
        final AiAgentOperationDO operation = new AiAgentOperationDO();
        final ConceptTableDO table = new ConceptTableDO();
        final DatasourceRespDTO datasource = new DatasourceRespDTO();
        final PropertyDO number = property(11L, "amount", "decimal");
        final PropertyDO date = property(12L, "business_date", "date");
        final PropertyDO unmapped = property(13L, "not_bound", "decimal");
        final List<PropertyColumnDO> columns = new ArrayList<>();
        Fixture() throws Exception {
            operation.setId("operation"); operation.setCreatorId(3L); operation.setSpaceId(4L);
            operation.setSpaceCode("space"); operation.setOntologyId(1L);
            ConceptDO concept = new ConceptDO(); concept.setId(2L); concept.setOntologyId(1L); concept.setName("订单");
            table.setId(10L); table.setConceptId(2L); table.setDatasourceId(20L);
            table.setDatabaseName("warehouse"); table.setTableName("ads_orders"); datasource.setDatasourceType("DORIS");
            columns.add(column(11L, "amount")); columns.add(column(12L, "business_date"));
            inject(drafts, "support", new MetricAgentToolSupport() {
                @Override public void assertOntology(Long id, AgentToolContext context) {
                    if (!Long.valueOf(1).equals(id)) throw new IllegalArgumentException("wrong ontology");
                }
                @Override public Long currentOntology(AgentToolContext context) { return 1L; }
            });
            inject(drafts, "operationService", stub(IAiAgentOperationService.class, (name, args) -> operation));
            inject(drafts, "conceptMapper", stub(ConceptMapper.class, (name, args) -> concept));
            inject(drafts, "conceptTableMapper", stub(ConceptTableMapper.class, (name, args) ->
                    "selectByConceptId".equals(name) ? Collections.singletonList(table) : table));
            inject(drafts, "propertyMapper", stub(PropertyMapper.class, (name, args) -> {
                if ("selectList".equals(name)) return Arrays.asList(number, date, unmapped);
                for (PropertyDO property : Arrays.asList(number, date, unmapped)) if (property.getId().equals(args[0])) return property;
                return null;
            }));
            inject(drafts, "propertyColumnMapper", stub(PropertyColumnMapper.class, (name, args) -> columns));
            inject(drafts, "datasourceApiService", stub(IDatasourceApiService.class, (name, args) ->
                    "hasDatasourceSpace".equals(name) ? true : datasource));
        }
        MetricDraftToolRequest.Preview request() {
            MetricDraftToolRequest.Preview request = new MetricDraftToolRequest.Preview();
            request.setOntologyId("1"); request.setConceptId("2"); return request;
        }
        MetricDraftToolRequest.Candidate candidate() {
            MetricDraftToolRequest.Candidate value = new MetricDraftToolRequest.Candidate();
            value.setCode("candidate_total"); value.setName("总额候选"); value.setDescription("候选口径待业务确认");
            value.setAggregation("SUM"); value.setValuePropertyId("11"); value.setTimePropertyId("12");
            value.setConceptTableId("10"); return value;
        }
        private static PropertyDO property(Long id, String code, String type) {
            PropertyDO row = new PropertyDO(); row.setId(id); row.setConceptId(2L); row.setCode(code); row.setName(code); row.setDataType(type); return row;
        }
        private static PropertyColumnDO column(Long propertyId, String column) {
            PropertyColumnDO row = new PropertyColumnDO(); row.setConceptTableId(10L); row.setPropertyId(propertyId); row.setColumnName(column); return row;
        }
    }
}
