package com.datamaster.module.ontology.service.impl;

import com.datamaster.common.database.DbQuery;
import com.datamaster.common.database.constants.DbQueryProperty;
import com.datamaster.common.database.core.DbColumn;
import com.datamaster.module.assets.api.governance.dto.AssetsTableGovernanceRespDTO;
import com.datamaster.module.assets.api.service.governance.IAssetsTableGovernanceApiService;
import com.datamaster.module.governance.api.flow.ISpaceFlowApi;
import com.datamaster.module.ontology.controller.admin.objectinstance.vo.RelationJumpReqVO;
import com.datamaster.module.ontology.dal.dataobject.*;
import org.junit.Test;
import java.lang.reflect.*;
import java.util.*;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class RelationQueryRestrictionTest {
    @Test public void userOrGroupsCannotEscapeMandatoryRelationEndpoint() throws Exception {
        ObjectInstanceQueryServiceImpl service = new ObjectInstanceQueryServiceImpl(); DbQuery query = mock(DbQuery.class);
        ISpaceFlowApi flows = mock(ISpaceFlowApi.class); IAssetsTableGovernanceApiService governance = mock(IAssetsTableGovernanceApiService.class);
        com.datamaster.module.ontology.service.analysis.ConceptTableConsumptionGuard identity = mock(com.datamaster.module.ontology.service.analysis.ConceptTableConsumptionGuard.class);
        when(identity.physicalTable(any(), any())).thenReturn("orders"); inject(service, "sourceIdentity", identity);
        inject(service, "spaceFlows", flows); inject(service, "tableGovernanceApiService", governance);
        when(flows.policy(1L, "space01")).thenReturn(new ISpaceFlowApi.Policy());
        AssetsTableGovernanceRespDTO allowed = new AssetsTableGovernanceRespDTO(); allowed.setAccessAllowed(true); when(governance.resolveTable(any())).thenReturn(allowed);
        DbColumn id = new DbColumn(); id.setColName("id"); id.setDataType("integer");
        when(query.getTableColumns(any(DbQueryProperty.class), eq("orders"))).thenReturn(List.of(id));
        when(query.queryByPage(anyString(), anyMap(), eq(0L), eq(20L), eq(0))).thenReturn(new com.datamaster.common.database.core.PageResult<>(0, List.of()));
        RelationJumpReqVO request = new RelationJumpReqVO(); request.setSpaceId(1L); request.setSpaceCode("space01"); request.setPageNum(1); request.setPageSize(20);
        request.setFilters("{\"groups\":[{\"filters\":[{\"field\":\"id\",\"op\":\"eq\",\"value\":1}]},{\"connector\":\"OR\",\"filters\":[{\"field\":\"id\",\"op\":\"eq\",\"value\":2}]}]}");
        ConceptTableDO table = new ConceptTableDO(); table.setId(40L); table.setConceptId(30L); table.setDatasourceId(9L); table.setTableName("orders");
        RelationColumnDO relation = new RelationColumnDO(); relation.setTargetColumn("id");
        Method method = service.getClass().getDeclaredMethod("doRelatedQuery", RelationJumpReqVO.class, RelationColumnDO.class, ConceptTableDO.class, ConceptDO.class, List.class, List.class, DbQuery.class, DbQueryProperty.class); method.setAccessible(true);
        method.invoke(service, request, relation, table, null, List.of(), List.of(3), query, new DbQueryProperty("MySql", "unused", "reader", null, 1, "business", null));
        verify(query).queryByPage(argThat(sql -> sql.contains("WHERE ((id = :p0) OR (id = :p1)) AND id IN (:rel0)")), argThat(params -> Integer.valueOf(3).equals(params.get("rel0"))), eq(0L), eq(20L), eq(0));
    }
    private static void inject(Object bean, String name, Object value) throws Exception { Field field = bean.getClass().getDeclaredField(name); field.setAccessible(true); field.set(bean, value); }
}
