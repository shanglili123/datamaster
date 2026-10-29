package com.datamaster.module.ontology.service.analysis;

import com.datamaster.module.assets.api.governance.dto.AssetsTableGovernanceRespDTO;
import com.datamaster.module.assets.dal.dataobject.assetColumn.AssetsAssetColumnDO;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
import static com.datamaster.module.ontology.service.analysis.MetricQueryBuilderTest.rejected;

public class MetricQuerySafetyTest {
    @Test public void deploymentIsClosedByDefaultAndAllowlistsAreStrict() {
        MetricQueryPolicy disabled = new MetricQueryPolicy(false, "", "", 15, 4);
        rejected(disabled::assertEnabled);
        MetricQueryPolicy policy = new MetricQueryPolicy(true, "40", "ads_metric_reader", 15, 1);
        policy.assertBinding(40L); policy.assertReadonlyUsername("ads_metric_reader");
        rejected(() -> policy.assertBinding(41L)); rejected(() -> policy.assertReadonlyUsername("root"));
        assertTrue(policy.tryAcquire()); assertFalse(policy.tryAcquire()); policy.release(); assertTrue(policy.tryAcquire()); policy.release();
        for (String text : Arrays.asList("1,", "1,,2", "1,x", "0", "-1", "1.0", "1,1", "9223372036854775808")) {
            rejected(() -> MetricQueryPolicy.parseBindingIds(text));
        }
        rejected(() -> new MetricQueryPolicy(true, "40", "reader", 61, 4));
        rejected(() -> new MetricQueryPolicy(true, "40", "reader", 15, 0));
        rejected(() -> new MetricQueryPolicy(true, "40", "reader", 15, 17));
    }
    @Test public void rejectsGovernanceOffWarnCatalogAccessPartialAndWrongIdentity() {
        Set<String> used = new LinkedHashSet<>(Arrays.asList("amount", "region"));
        MetricQuerySourceGuard.assertGovernance(response(), used, 50L, "ads_orders");
        AssetsTableGovernanceRespDTO off = response(); off.setEnabled(false);
        rejected(() -> MetricQuerySourceGuard.assertGovernance(off, used, 50L, "ads_orders"));
        AssetsTableGovernanceRespDTO warn = response(); warn.setMode("warn");
        rejected(() -> MetricQuerySourceGuard.assertGovernance(warn, used, 50L, "ads_orders"));
        AssetsTableGovernanceRespDTO catalog = response(); catalog.setSource("CATALOG");
        rejected(() -> MetricQuerySourceGuard.assertGovernance(catalog, used, 50L, "ads_orders"));
        AssetsTableGovernanceRespDTO denied = response(); denied.setAccessAllowed(false);
        rejected(() -> MetricQuerySourceGuard.assertGovernance(denied, used, 50L, "ads_orders"));
        AssetsTableGovernanceRespDTO partial = response(); partial.setAllowedColumns(Collections.singletonList("amount"));
        rejected(() -> MetricQuerySourceGuard.assertGovernance(partial, used, 50L, "ads_orders"));
        AssetsTableGovernanceRespDTO empty = response(); empty.setAllowedColumns(Collections.emptyList());
        rejected(() -> MetricQuerySourceGuard.assertGovernance(empty, used, 50L, "ads_orders"));
        AssetsTableGovernanceRespDTO hasDenied = response(); hasDenied.setDeniedColumns(Collections.singletonList("unrelated"));
        rejected(() -> MetricQuerySourceGuard.assertGovernance(hasDenied, used, 50L, "ads_orders"));
        rejected(() -> MetricQuerySourceGuard.assertGovernance(response(), used, 51L, "ads_orders"));
    }
    @Test public void rejectsSensitiveUnregisteredAndAmbiguousColumns() {
        Set<String> used = new LinkedHashSet<>(Arrays.asList("amount", "event_time"));
        List<AssetsAssetColumnDO> columns = Arrays.asList(column("amount", null), column("event_time", null));
        MetricQuerySourceGuard.assertNonsensitiveColumns(used, columns);
        rejected(() -> MetricQuerySourceGuard.assertNonsensitiveColumns(used, Collections.singletonList(column("amount", null))));
        rejected(() -> MetricQuerySourceGuard.assertNonsensitiveColumns(used, Arrays.asList(column("amount", null), column("event_time", 1L))));
        rejected(() -> MetricQuerySourceGuard.assertNonsensitiveColumns(used, Arrays.asList(column("amount", null), column("event_time", null), column("AMOUNT", null))));
    }
    private static AssetsTableGovernanceRespDTO response() {
        AssetsTableGovernanceRespDTO r = new AssetsTableGovernanceRespDTO(); r.setEnabled(true); r.setMode("strict");
        r.setSource("ASSET"); r.setAssetId(60L); r.setDatasourceId(50L); r.setTableName("ads_orders"); r.setAccessAllowed(true); return r;
    }
    private static AssetsAssetColumnDO column(String name, Long level) {
        AssetsAssetColumnDO result = new AssetsAssetColumnDO(); result.setColumnName(name); result.setSensitiveLevelId(level); return result;
    }
}
