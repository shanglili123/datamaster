package com.datamaster.module.governance.service.flow;

import com.alibaba.fastjson2.JSON;
import com.datamaster.common.api.space.ISpaceApi;
import com.datamaster.common.core.domain.entity.SysUser;
import com.datamaster.common.core.domain.model.LoginUser;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.module.governance.api.flow.ISpaceFlowApi.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Field;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class SpaceFlowPublicationTest {
    private SpaceFlowService service;
    private JdbcTemplate jdbc;
    private ISpaceApi spaces;

    @Before public void setUp() throws Exception {
        LoginUser user = new LoginUser(7L, 1L, new SysUser(), Set.of("tax:space:edit"));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null));
        jdbc = mock(JdbcTemplate.class); spaces = mock(ISpaceApi.class); service = new SpaceFlowService();
        inject(service, "jdbc", jdbc); inject(service, "spaces", spaces);
        when(spaces.getSpaceCodeBySpaceId(1L)).thenReturn("space01");
        when(spaces.hasUserSpace(7L, 1L, "space01")).thenReturn(true);
        when(jdbc.queryForObject(contains("manager_id"), eq(Long.class), eq(1L))).thenReturn(7L);
    }
    @After public void tearDown() { SecurityContextHolder.clearContext(); }

    @Test public void spaceLookupUsesStoredStringDeleteFlag() {
        when(jdbc.queryForList(contains("SELECT revision,draft_json,published_version,published_json"), eq(1L))).thenReturn(List.of());
        assertEquals(0, service.get(1L, "space01").get("revision"));
        verify(jdbc).queryForObject(contains("del_flag='0'"), eq(Long.class), eq(1L));
    }

    @Test public void previewUsesSavedDefinitionRatherThanClientCandidate() {
        Map<String,Object> current = new HashMap<>();
        current.put("revision", 4); current.put("published_version", 2);
        current.put("published_json", JSON.toJSONString(chain("METADATA", "ASSET")));
        current.put("draft_json", JSON.toJSONString(chain("METADATA", "ASSET", "QUALITY")));
        when(jdbc.queryForList(contains("SELECT revision,draft_json,published_version,published_json"), eq(1L))).thenReturn(List.of(current));
        SaveRequest request = request(4); request.setDefinition(chain("METADATA"));
        Map<String,Object> preview = service.preview(request);
        assertEquals(List.of("QUALITY"), preview.get("addedRequirements"));
        assertEquals(3, preview.get("nextVersion")); assertEquals(true, preview.get("qualityEvidenceReset"));
        assertEquals(false, preview.get("consumerImpactKnown"));
        List<?> inventories = (List<?>) preview.get("inventories");
        assertEquals(2, inventories.size());
        assertEquals(false, ((Map<?,?>) inventories.get(0)).get("available"));
        assertNull(((Map<?,?>) inventories.get(0)).get("total"));
        verify(jdbc).queryForObject(contains("manager_id"), eq(Long.class), eq(1L));
        verify(jdbc).queryForList(contains("SELECT revision,draft_json,published_version,published_json"), eq(1L));
        verifyNoMoreInteractions(jdbc);
    }
    @Test public void previewRejectsStaleOrMissingRevision() {
        when(jdbc.queryForList(contains("SELECT revision,draft_json,published_version,published_json"), eq(1L)))
                .thenReturn(List.of(Map.of("revision", 5)));
        reject(() -> service.preview(request(4)));
        reject(() -> service.preview(request(null)));
    }
    @Test public void publicationRejectsStaleRevisionWhileHoldingFlowLock() {
        when(jdbc.queryForList(contains("FOR UPDATE"), eq(1L))).thenReturn(List.of(Map.of("revision", 5)));
        reject(() -> service.publish(request(4)));
        verify(jdbc).queryForObject(contains("manager_id"), eq(Long.class), eq(1L));
        verify(jdbc).queryForList(contains("FOR UPDATE"), eq(1L)); verifyNoMoreInteractions(jdbc);
    }
    @Test public void historyIsSpaceScopedAndReturnsStructuredDefinitions() {
        when(jdbc.queryForObject(contains("count(*) FROM gov_space_flow_version"), eq(Long.class), eq(1L))).thenReturn(12L);
        when(jdbc.queryForList(contains("ORDER BY version DESC LIMIT ? OFFSET ?"), eq(1L), eq(10), eq(10)))
                .thenReturn(List.of(Map.of("version", 2, "definition_json", JSON.toJSONString(chain("METADATA", "ASSET")), "published_by", 7L)));
        Map<String,Object> result = service.versions(1L, "space01", 2, 10);
        assertEquals(12L, result.get("total"));
        Map<?,?> version = (Map<?,?>) ((List<?>) result.get("rows")).get(0);
        assertTrue(version.get("definition") instanceof Definition); assertFalse(version.containsKey("definition_json"));
        verify(jdbc).queryForList(contains("ORDER BY version DESC LIMIT ? OFFSET ?"), eq(1L), eq(10), eq(10));
    }
    @Test public void deniesOtherSpaceBeforeReadingVersions() {
        when(spaces.hasUserSpace(7L, 1L, "space01")).thenReturn(false);
        reject(() -> service.versions(1L, "space01", 1, 10)); verifyNoInteractions(jdbc);
    }
    @Test public void consumerPageUsesSavedDraftAndNextVersionForReadiness() throws Exception {
        com.datamaster.module.governance.api.flow.SpaceFlowImpactProvider provider = mock(com.datamaster.module.governance.api.flow.SpaceFlowImpactProvider.class);
        when(provider.type()).thenReturn("API");
        com.datamaster.module.governance.api.flow.SpaceFlowImpactProvider.Consumer consumer = new com.datamaster.module.governance.api.flow.SpaceFlowImpactProvider.Consumer(); consumer.setId(10L);
        com.datamaster.module.governance.api.flow.SpaceFlowImpactProvider.Page inventory = new com.datamaster.module.governance.api.flow.SpaceFlowImpactProvider.Page(); inventory.setTotal(21); inventory.setRows(List.of(consumer));
        when(provider.inventory(1L, "space01", 2, 10)).thenReturn(inventory);
        SpaceFlowReadinessService readiness = mock(SpaceFlowReadinessService.class);
        inject(service, "impactProviders", List.of(provider)); inject(service, "readiness", readiness);
        when(jdbc.queryForList(contains("SELECT revision,draft_json,published_version FROM"), eq(1L)))
                .thenReturn(List.of(Map.of("revision", 4, "published_version", 2, "draft_json", JSON.toJSONString(chain("METADATA", "ASSET")))));
        SaveRequest request = request(4); request.setDefinition(chain("METADATA"));
        Map<String,Object> result = service.impacts(request, "API", 2, 10);
        assertEquals(21L, result.get("total"));
        verify(readiness).assess(eq(consumer), argThat(candidate -> candidate.getNodes().contains("ASSET")), eq(1L), eq("space01"), eq(3));
        verify(provider).inventory(1L, "space01", 2, 10);
    }
    @Test public void consumerPageRejectsStaleRevisionBeforeInventory() throws Exception {
        com.datamaster.module.governance.api.flow.SpaceFlowImpactProvider provider = mock(com.datamaster.module.governance.api.flow.SpaceFlowImpactProvider.class);
        inject(service, "impactProviders", List.of(provider));
        when(jdbc.queryForList(contains("SELECT revision,draft_json,published_version FROM"), eq(1L))).thenReturn(List.of(Map.of("revision", 5)));
        reject(() -> service.impacts(request(4), "API", 1, 10)); verifyNoInteractions(provider);
    }
    @Test public void publicationHistoryInsertFailureNeverUpdatesActiveFlow() {
        when(jdbc.queryForList(contains("FOR UPDATE"), eq(1L))).thenReturn(List.of(Map.of("revision", 4, "published_version", 2, "draft_json", JSON.toJSONString(chain("METADATA", "ASSET")))));
        reject(() -> service.publish(request(4)));
        verify(jdbc).update(contains("INSERT INTO gov_space_flow_version"), eq(1L), eq(3), anyString(), eq(7L));
        verify(jdbc, never()).update(contains("UPDATE gov_space_flow SET published_json"), anyString(), anyInt(), anyLong(), anyLong());
    }
    @Test public void publicationStateUpdateFailureIsReportedInsteadOfReturningSuccess() {
        when(jdbc.queryForList(contains("FOR UPDATE"), eq(1L))).thenReturn(List.of(Map.of("revision", 4, "published_version", 2, "draft_json", JSON.toJSONString(chain("METADATA", "ASSET")))));
        when(jdbc.update(contains("INSERT INTO gov_space_flow_version"), eq(1L), eq(3), anyString(), eq(7L))).thenReturn(1);
        reject(() -> service.publish(request(4)));
        verify(jdbc).update(contains("UPDATE gov_space_flow SET published_json"), anyString(), eq(3), eq(7L), eq(1L));
        verify(jdbc, never()).queryForList(contains("SELECT revision,draft_json,published_version,published_json"), eq(1L));
    }
    @Test public void malformedDraftAndHistoryNeverReachEditorAsValidDefinitions() {
        for (String invalid : List.of("broken", "null", "{}", "{\"nodes\":[\"UNKNOWN\"],\"edges\":[]}")) {
            when(jdbc.queryForList(contains("SELECT revision,draft_json,published_version,published_json"), eq(1L)))
                    .thenReturn(List.of(Map.of("revision", 1, "published_version", 0, "draft_json", invalid)));
            reject(() -> service.get(1L, "space01"));
            when(jdbc.queryForList(contains("ORDER BY version DESC LIMIT ? OFFSET ?"), eq(1L), eq(10), eq(0)))
                    .thenReturn(List.of(Map.of("version", 1, "definition_json", invalid)));
            reject(() -> service.versions(1L, "space01", 1, 10));
        }
    }
    @Test public void positivePublicationVersionWithMissingDefinitionCannotBecomeUnconfiguredPolicy() {
        when(jdbc.queryForList(contains("SELECT published_version,published_json"), eq(1L))).thenReturn(List.of(Map.of("published_version", 2)));
        reject(() -> service.policy(1L, "space01"));
        when(jdbc.queryForList(contains("SELECT published_version,published_json"), eq(1L))).thenReturn(List.of(Map.of("published_version", 0)));
        assertFalse(service.policy(1L, "space01").isConfigured());
        when(jdbc.queryForList(contains("SELECT published_version,published_json"), eq(1L))).thenReturn(List.of(Map.of("published_version", 0, "published_json", JSON.toJSONString(chain("METADATA")))));
        reject(() -> service.policy(1L, "space01"));
    }
    @Test public void validLegacyDefinitionWithoutLayoutCanStillBeRead() {
        String definition = "{\"nodes\":[\"METADATA\",\"API\",\"ANALYSIS\"],\"edges\":[{\"from\":\"METADATA\",\"to\":\"API\"},{\"from\":\"METADATA\",\"to\":\"ANALYSIS\"}]}";
        when(jdbc.queryForList(contains("SELECT revision,draft_json,published_version,published_json"), eq(1L)))
                .thenReturn(List.of(Map.of("revision", 1, "published_version", 0, "draft_json", definition)));
        Map<String,Object> result = service.get(1L, "space01");
        assertTrue(result.get("draft") instanceof Definition); assertNull(result.get("published"));
    }
    @Test public void corruptStoredDraftCannotBePublishedOrUsedForPreview() {
        Map<String,Object> invalid = Map.of("revision", 4, "published_version", 0, "draft_json", "null");
        when(jdbc.queryForList(contains("SELECT revision,draft_json,published_version,published_json"), eq(1L))).thenReturn(List.of(invalid));
        when(jdbc.queryForList(contains("FOR UPDATE"), eq(1L))).thenReturn(List.of(invalid));
        reject(() -> service.preview(request(4))); reject(() -> service.publish(request(4)));
        verify(jdbc, never()).update(contains("INSERT INTO gov_space_flow_version"), anyLong(), anyInt(), anyString(), anyLong());
        verify(jdbc, never()).update(contains("UPDATE gov_space_flow SET published_json"), anyString(), anyInt(), anyLong(), anyLong());
    }
    @Test public void successfulPublicationPersistsHistoryThenActiveVersionAndReturnsNewRevision() {
        String definition = JSON.toJSONString(chain("METADATA", "ASSET"));
        when(jdbc.queryForList(contains("FOR UPDATE"), eq(1L))).thenReturn(List.of(Map.of("revision", 4, "published_version", 2, "draft_json", definition)));
        when(jdbc.update(contains("INSERT INTO gov_space_flow_version"), eq(1L), eq(3), eq(definition), eq(7L))).thenReturn(1);
        when(jdbc.update(contains("UPDATE gov_space_flow SET published_json"), eq(definition), eq(3), eq(7L), eq(1L))).thenReturn(1);
        when(jdbc.queryForList(contains("SELECT revision,draft_json,published_version,published_json"), eq(1L)))
                .thenReturn(List.of(Map.of("revision", 5, "published_version", 3, "draft_json", definition, "published_json", definition)));
        Map<String,Object> result = service.publish(request(4));
        assertEquals(5, result.get("revision")); assertEquals(3, result.get("publishedVersion")); assertTrue(result.get("published") instanceof Definition);
        org.mockito.InOrder order = inOrder(jdbc);
        order.verify(jdbc).queryForObject(contains("manager_id"), eq(Long.class), eq(1L));
        order.verify(jdbc).queryForList(contains("FOR UPDATE"), eq(1L));
        order.verify(jdbc).update(contains("INSERT INTO gov_space_flow_version"), eq(1L), eq(3), eq(definition), eq(7L));
        order.verify(jdbc).update(contains("UPDATE gov_space_flow SET published_json"), eq(definition), eq(3), eq(7L), eq(1L));
    }
    @Test public void exhaustedVersionOrRevisionNeverWritesWrappedCounters() {
        String definition = JSON.toJSONString(chain("METADATA"));
        SaveRequest save = request(Integer.MAX_VALUE); save.setDefinition(chain("METADATA")); reject(() -> service.save(save));
        when(jdbc.queryForList(contains("FOR UPDATE"), eq(1L)))
                .thenReturn(List.of(Map.of("revision", 4, "published_version", Integer.MAX_VALUE, "draft_json", definition)));
        reject(() -> service.publish(request(4)));
        when(jdbc.queryForList(contains("FOR UPDATE"), eq(1L)))
                .thenReturn(List.of(Map.of("revision", Integer.MAX_VALUE, "published_version", 2, "draft_json", definition)));
        reject(() -> service.publish(request(Integer.MAX_VALUE)));
        verify(jdbc, never()).update(anyString(), anyLong(), anyInt(), anyString(), anyLong());
    }
    private static SaveRequest request(Integer revision) {
        SaveRequest request = new SaveRequest(); request.setSpaceId(1L); request.setSpaceCode("space01"); request.setRevision(revision); return request;
    }
    private static Definition chain(String... names) {
        Definition d = new Definition(); d.setNodes(new ArrayList<>(Arrays.asList(names))); d.getNodes().addAll(List.of("API", "ANALYSIS"));
        for (int i = 1; i < names.length; i++) d.getEdges().add(edge(names[i - 1], names[i]));
        d.getEdges().add(edge(names[names.length - 1], "API")); d.getEdges().add(edge(names[names.length - 1], "ANALYSIS")); return d;
    }
    private static Edge edge(String from, String to) { Edge edge = new Edge(); edge.setFrom(from); edge.setTo(to); return edge; }
    private static void reject(Runnable action) {
        try { action.run(); fail("Must reject invalid publication request"); } catch (ServiceException expected) { assertNotNull(expected.getMessage()); }
    }
    private static void inject(Object bean, String name, Object value) throws Exception {
        Field field = bean.getClass().getDeclaredField(name); field.setAccessible(true); field.set(bean, value);
    }
}
