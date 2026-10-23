package com.datamaster.metadata.service.lineage;

import com.datamaster.common.api.space.ISpaceApi;
import com.datamaster.common.core.domain.entity.SysRole;
import com.datamaster.common.core.domain.model.LoginUser;
import com.datamaster.common.security.AccessPolicy;
import com.datamaster.common.utils.SecurityUtils;
import com.datamaster.metadata.api.lineage.dto.LineageQueryReqDTO;
import com.datamaster.metadata.dal.dataobject.lineage.LineageRows.ObjectRow;
import com.datamaster.metadata.dal.dataobject.metadata.CatalogTableDO;
import com.datamaster.metadata.dal.dataobject.metadata.CatalogColumnDO;
import com.datamaster.metadata.dal.mapper.lineage.LineageAccessMapper;
import com.datamaster.common.database.constants.DbQueryProperty;
import com.datamaster.module.assets.api.datasource.dto.AssetsDatasourceRespDTO;
import com.datamaster.module.assets.api.service.asset.IAssetsDatasourceApiService;
import com.datamaster.module.assets.dal.dataobject.asset.AssetsAssetDO;
import com.datamaster.security.web.service.PermissionService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import javax.annotation.Resource;
import java.util.*;

/** Hard authorization regardless of legacy off/warn switches. No permission creation/grants. */
@Component
public class LineageGuard {
    @Resource private ISpaceApi spaceApi;
    @Resource private PermissionService permissions;
    @Resource private LineageAccessMapper access;
    @Resource private IAssetsDatasourceApiService datasourceApi;

    public static final class Visible {
        public final CatalogTableDO table;
        public final Long assetId;
        Visible(CatalogTableDO table, Long assetId) { this.table = table; this.assetId = assetId; }
    }

    public void context(LineageQueryReqDTO request, String permission) {
        if (request == null || request.getSpaceId() == null) deny();
        String code = request.getSpaceCode();
        if (code == null || code.trim().isEmpty()) code = request.getCode();
        if (code == null || code.trim().isEmpty()) deny();
        if (request.getCode() != null && !code.equals(request.getCode())) deny();
        if (!permissions.hasPermi(permission)) deny();
        if (!spaceApi.hasUserSpace(SecurityUtils.getUserId(), request.getSpaceId(), code)) deny();
        request.setSpaceCode(code); request.setCode(code);
    }

    public CatalogTableDO metadata(Long id, LineageQueryReqDTO request) {
        CatalogTableDO table = access.metadata(id);
        if (!metadataAllowed(table, request)) deny();
        return table;
    }

    public Visible visible(ObjectRow object, LineageQueryReqDTO request, boolean assetMode) {
        if (object == null || !"TABLE".equals(object.getKind()) || !connection(object.getDatasourceId(), request)) return null;
        if (assetMode) {
            if (!permissions.hasPermi("ast:asset:query")) return null;
            Map<Long, AssetsAssetDO> candidates = new LinkedHashMap<>();
            for (AssetsAssetDO asset : access.assetsForObject(object, request.getSpaceId(), request.getSpaceCode())) candidates.put(asset.getId(), asset);
            for (AssetsAssetDO asset : access.unlinkedAssets(object.getDatasourceId(), object.getObjectName(), request.getSpaceId(), request.getSpaceCode())) {
                CatalogTableDO resolved = assetMetadata(asset, request);
                if (sameIdentity(resolved, object)) candidates.put(asset.getId(), asset);
            }
            if (candidates.size() != 1) return null;
            AssetsAssetDO asset = candidates.values().iterator().next();
            CatalogTableDO table = assetMetadata(asset, request);
            if (!sameIdentity(table, object)) return null;
            return new Visible(table, asset.getId());
        }
        if (!permissions.hasPermi("cat:table:query")) return null;
        List<CatalogTableDO> matches = access.metadataForObject(object, request.getSpaceId(), request.getSpaceCode());
        if (matches.size() != 1 || !metadataAllowed(matches.get(0), request)) return null;
        return new Visible(matches.get(0), null);
    }

    public AssetsAssetDO asset(Long id, LineageQueryReqDTO request) {
        if (!permissions.hasPermi("ast:asset:query")) deny();
        AssetsAssetDO asset = access.asset(id, request.getSpaceId(), request.getSpaceCode());
        if (asset == null || !connection(asset.getDatasourceId(), request)) deny();
        return asset;
    }

    public CatalogTableDO assetMetadata(AssetsAssetDO asset, LineageQueryReqDTO request) {
        if (asset.getTableId() != null) {
            CatalogTableDO linked = access.metadata(asset.getTableId());
            if (linked == null || !Objects.equals(linked.getDatasourceId(), asset.getDatasourceId())) return null;
            return linked;
        }
        CatalogTableDO registered = registeredIdentity(asset, request);
        if (registered == null) return null;
        ObjectRow object = new ObjectRow(); object.setDatasourceId(registered.getDatasourceId());
        object.setCatalogName(registered.getDbName()); object.setSchemaName(registered.getSchemaName()); object.setObjectName(registered.getTableName());
        List<CatalogTableDO> metadata = access.metadataForObject(object, request.getSpaceId(), request.getSpaceCode());
        if (metadata.size() > 1) return null;
        return metadata.isEmpty() ? registered : metadata.get(0);
    }

    private CatalogTableDO registeredIdentity(AssetsAssetDO asset, LineageQueryReqDTO request) {
        // AST_ASSET stores the physical simple table name, not a SQL expression or qualified token.
        String name = asset.getTableName();
        if (name == null || !name.matches("[\\p{L}_][\\p{L}\\p{N}_$]{0,511}")) return null;
        try {
            AssetsDatasourceRespDTO datasource = datasourceApi.getDatasourceById(asset.getDatasourceId());
            if (datasource == null || !Objects.equals(datasource.getId(), asset.getDatasourceId())) return null;
            // This constructor only parses existing registration; no DbQuery, JDBC URL or remote call.
            DbQueryProperty property = new DbQueryProperty(datasource.getDatasourceType(), datasource.getIp(), datasource.getPort(), datasource.getDatasourceConfig());
            String type = property.getDbType() == null ? "" : property.getDbType().toLowerCase(Locale.ROOT);
            String catalog = nullIfEmpty(property.getDbName());
            String schema;
            if (type.contains("postgres") || type.contains("kingbase")) {
                schema = nullIfEmpty(property.getSid());
                if (catalog == null || schema == null) return null;
            } else if (type.contains("mysql") || type.contains("mariadb") || type.contains("doris")) {
                schema = null;
                if (catalog == null) return null;
            } else {
                // Oracle SID, SQLServer search paths etc are not reliable schema identities here.
                return null;
            }
            CatalogTableDO table = new CatalogTableDO();
            table.setDatasourceId(asset.getDatasourceId()); table.setTableName(name);
            table.setDbName(catalog); table.setSchemaName(schema);
            table.setSpaceId(request.getSpaceId()); table.setSpaceCode(request.getSpaceCode());
            return table;
        } catch (RuntimeException e) {
            // Registration unresolved: never log config/host/password or use a bare-name fallback.
            return null;
        }
    }

    private static boolean sameIdentity(CatalogTableDO table, ObjectRow object) {
        return table != null && Objects.equals(table.getDatasourceId(), object.getDatasourceId())
                && Objects.equals(nullIfEmpty(table.getDbName()), object.getCatalogName())
                && Objects.equals(nullIfEmpty(table.getSchemaName()), object.getSchemaName())
                && Objects.equals(table.getTableName(), object.getObjectName());
    }
    private static String nullIfEmpty(String value) { return value == null || value.isEmpty() ? null : value; }

    private boolean connection(Long datasourceId, LineageQueryReqDTO request) {
        return datasourceId != null && access.connectionInSpace(datasourceId, request.getSpaceId(), request.getSpaceCode()) > 0;
    }

    public boolean columnVisible(ObjectRow column, Visible parent, LineageQueryReqDTO request, boolean assetMode) {
        if (column == null || parent == null || !"COLUMN".equals(column.getKind())
                || column.getColumnName() == null || !Objects.equals(column.getDatasourceId(), parent.table.getDatasourceId())) return false;
        if (assetMode) {
            // Only an explicit positive column-space relation grants visibility. Off/warn is irrelevant.
            return parent.assetId != null && permissions.hasPermi("ast:asset:query")
                    && access.assetColumnVisible(parent.assetId, column.getColumnName(), request.getSpaceId(), request.getSpaceCode()) == 1;
        }
        if (parent.table.getId() == null || !permissions.hasPermi("cat:table:query")) return false;
        List<CatalogColumnDO> columns = access.metadataColumns(parent.table.getId(), parent.table.getDatasourceId(),
                column.getColumnName(), request.getSpaceId(), request.getSpaceCode());
        if (columns.size() != 1) return false;
        CatalogColumnDO physical = columns.get(0);
        if (!dataScopeAllowed(physical.getBusinessLeader(), physical.getResponsibleDept(), request)) return false;
        ObjectRow tableIdentity = new ObjectRow();
        tableIdentity.setDatasourceId(parent.table.getDatasourceId());
        tableIdentity.setCatalogName(parent.table.getDbName() == null || parent.table.getDbName().isEmpty() ? null : parent.table.getDbName());
        tableIdentity.setSchemaName(parent.table.getSchemaName() == null || parent.table.getSchemaName().isEmpty() ? null : parent.table.getSchemaName());
        tableIdentity.setObjectName(parent.table.getTableName());
        List<AssetsAssetDO> assets = access.assetsForObject(tableIdentity, request.getSpaceId(), request.getSpaceCode());
        // No reliable column policy for an unregistered/ambiguous asset: hide, never infer ALL from absence.
        return assets.size() == 1 && access.assetColumnVisible(assets.get(0).getId(), column.getColumnName(),
                request.getSpaceId(), request.getSpaceCode()) == 1;
    }

    private boolean metadataAllowed(CatalogTableDO table, LineageQueryReqDTO request) {
        if (table == null || !Objects.equals(table.getSpaceId(), request.getSpaceId())
                || !Objects.equals(table.getSpaceCode(), request.getSpaceCode()) || !connection(table.getDatasourceId(), request)) return false;
        if (!permissions.hasPermi("cat:table:query")) return false;
        return dataScopeAllowed(table.getBusinessLeader(), table.getResponsibleDept(), request);
    }

    private boolean dataScopeAllowed(Long businessLeader, Long responsibleDept, LineageQueryReqDTO request) {
        LoginUser login = SecurityUtils.getLoginUser();
        if (login.getUser() == null) return false;
        if (AccessPolicy.isPlatformAdmin(login.getUserId(), login.getUser().getRoles())) return true;
        List<SysRole> roles = login.getUser().getRoles();
        if (roles == null) return false;
        // Metadata list's owner/department semantics, derived server-side only. Missing role scope denies.
        for (SysRole role : roles) {
            if (role == null || !"0".equals(role.getStatus())
                    || (!Objects.equals(role.getSpaceId(), request.getSpaceId()) && !Long.valueOf(0).equals(role.getSpaceId()))
                    || role.getPermissions() == null
                    || !(SecurityUtils.hasPermi(role.getPermissions(), "cat:table:list")
                        || SecurityUtils.hasPermi(role.getPermissions(), "cat:table:query"))) continue;
            String mode = role.getDataScope();
            if ("1".equals(mode)) return true;
            if ("5".equals(mode) && Objects.equals(login.getUserId(), businessLeader)) return true;
            Long dept = responsibleDept;
            if (dept == null) continue;
            if ("3".equals(mode) && Objects.equals(login.getDeptId(), dept)) return true;
            if ("4".equals(mode) && login.getDeptId() != null && access.childDepartment(dept, login.getDeptId()) > 0) return true;
            if ("2".equals(mode) && role.getRoleId() != null && access.roleDepartment(role.getRoleId(), dept) > 0) return true;
        }
        return false;
    }

    private static void deny() { throw new AccessDeniedException("LINEAGE_ACCESS_DENIED"); }
}
