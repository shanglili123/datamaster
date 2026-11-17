package com.datamaster.module.ontology.service.analysis;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.database.constants.DbQueryProperty;
import com.datamaster.common.datasource.mgmt.api.dto.DatasourceRespDTO;
import com.datamaster.common.datasource.mgmt.api.IDatasourceApiService;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.SecurityUtils;
import com.datamaster.module.ontology.controller.admin.metricsource.vo.MetricSourceApprovalSaveReqVO;
import com.datamaster.module.ontology.controller.admin.metric.vo.MetricRespVO;
import com.datamaster.module.ontology.dal.dataobject.OntologyDO;
import com.datamaster.module.ontology.dal.mapper.MetricMetadataLockMapper;
import lombok.Getter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import static com.datamaster.module.ontology.service.metric.MetricRules.require;

/** Business admission. All authoritative reads use the application database, never datasource caches. */
@Service
public class MetricSourceApprovalService {
    @Resource private JdbcTemplate jdbc;
    @Resource private MetricAnalysisAccessGuard access;
    @Resource private MetricQueryPolicy policy;
    @Resource private MetricMetadataLockMapper metadataLocks;
    @Resource private IDatasourceApiService datasourceApiService;

    private static final String SOURCE_SQL = "SELECT b.id AS binding_id,b.concept_id,b.datasource_id,b.database_name,b.schema_name,b.table_name,"
            + "b.source_auth_revision AS binding_auth_revision,c.ontology_id,c.name AS concept_name,"
            + "d.datasource_name,d.datasource_type,d.ip,d.port,d.datasource_config,d.source_auth_revision AS datasource_auth_revision "
            + "FROM ont_concept_table b JOIN ont_concept c ON c.id=b.concept_id "
            + "JOIN ast_datasource d ON d.id=b.datasource_id WHERE b.id=? AND c.ontology_id=? "
            + "AND b.del_flag=0 AND c.del_flag=0 AND (d.del_flag IS NULL OR d.del_flag='0') AND d.valid_flag='1'";
    private static final String BINDINGS = " FROM ont_concept_table b JOIN ont_concept c ON c.id=b.concept_id WHERE c.ontology_id=? AND b.del_flag=0 AND c.del_flag=0";

    /** Request-local capability: never serialized or accepted from an HTTP request. */
    @Getter
    public static final class ExecutionContext {
        private final Long ontologyId;
        private final Long bindingId;
        private final int approvalRevision;
        private final String sourceToken;
        private final DbQueryProperty property;
        private final DatasourceRespDTO datasource;
        ExecutionContext(Long ontologyId, Long bindingId, int approvalRevision, String sourceToken,
                         DbQueryProperty property, DatasourceRespDTO datasource) {
            this.ontologyId = ontologyId; this.bindingId = bindingId;
            this.approvalRevision = approvalRevision; this.sourceToken = sourceToken;
            this.property = property; this.datasource = datasource;
        }
    }

    public Map<String,Object> settings(Long ontologyId) {
        manager(ontologyId);
        return protections();
    }

    public PageResult<Map<String,Object>> page(Long ontologyId, int pageNum, int pageSize) {
        OntologyDO ontology = manager(ontologyId); pagination(pageNum, pageSize);
        Long total = jdbc.queryForObject("SELECT count(*)" + BINDINGS, Long.class, ontologyId);
        List<Map<String,Object>> candidates = jdbc.queryForList("SELECT b.id AS binding_id,c.id AS concept_id,c.name AS concept_name,b.datasource_id,b.database_name,b.schema_name,b.table_name"
                + BINDINGS + " ORDER BY b.id DESC LIMIT ? OFFSET ?", ontologyId,pageSize,(long)(pageNum-1)*pageSize);
        List<Map<String,Object>> rows = new ArrayList<>();
        for (Map<String,Object> candidate : candidates) {
            Long bindingId = number(candidate,"binding_id");
            Map<String,Object> record = approval(bindingId);
            Map<String,Object> row = new LinkedHashMap<>();
            row.put("bindingId",bindingId); row.put("conceptId",number(candidate,"concept_id")); row.put("conceptName",candidate.get("concept_name"));
            row.put("datasourceId",number(candidate,"datasource_id")); row.put("databaseName",candidate.get("database_name"));
            row.put("schemaName",candidate.get("schema_name")); row.put("tableName",candidate.get("table_name"));
            row.put("revision",record == null ? 0 : integer(record,"revision"));
            if (record != null) {
                row.put("reason",record.get("reason")); row.put("approvedBy",record.get("approved_by")); row.put("approvedAt",record.get("approved_at"));
                row.put("updatedBy",record.get("updated_by")); row.put("updatedAt",record.get("updated_at"));
            }
            List<Map<String,Object>> sources = jdbc.queryForList(SOURCE_SQL,bindingId,ontologyId);
            String token = null;
            if (sources.size() == 1) {
                Map<String,Object> source = sources.get(0);
                row.put("datasourceName",source.get("datasource_name")); row.put("datasourceType",source.get("datasource_type"));
                try {
                    String username = username(source);
                    token = token(source,ontology.getSpaceId(),username);
                    validateCandidate(source,username);
                    row.put("username",username); row.put("sourceToken",token);
                } catch (ServiceException | IllegalArgumentException ex) { token = null; }
            }
            String status = state(record,token,ontology);
            row.put("status",status); row.put("available",policy.isEnabled() && "APPROVED".equals(status));
            row.put("notice",notice(status));
            // A revoked/stale/unavailable source remains revocable using the recorded identity.
            if (!row.containsKey("sourceToken") && record != null) row.put("sourceToken",record.get("source_token"));
            rows.add(row);
        }
        return new PageResult<>(rows,total == null ? 0 : total);
    }

    public PageResult<Map<String,Object>> history(Long ontologyId, Long bindingId, int pageNum, int pageSize) {
        manager(ontologyId); bindingOwner(ontologyId,bindingId); pagination(pageNum,pageSize);
        Long total = jdbc.queryForObject("SELECT count(*) FROM ont_metric_source_approval_event WHERE ontology_id=? AND binding_id=?",Long.class,ontologyId,bindingId);
        List<Map<String,Object>> events = jdbc.queryForList("SELECT id,action,reason,actor_id,occurred_at,revision,username,datasource_id,database_name,schema_name,table_name FROM ont_metric_source_approval_event WHERE ontology_id=? AND binding_id=? ORDER BY id DESC LIMIT ? OFFSET ?",ontologyId,bindingId,pageSize,(long)(pageNum-1)*pageSize);
        List<Map<String,Object>> rows = new ArrayList<>();
        for (Map<String,Object> event : events) {
            Map<String,Object> row = new LinkedHashMap<>();
            row.put("id",event.get("id")); row.put("action",event.get("action")); row.put("reason",event.get("reason"));
            row.put("actorId",event.get("actor_id")); row.put("occurredAt",event.get("occurred_at")); row.put("revision",event.get("revision"));
            row.put("username",event.get("username")); row.put("datasourceId",event.get("datasource_id"));
            row.put("databaseName",event.get("database_name")); row.put("schemaName",event.get("schema_name")); row.put("tableName",event.get("table_name")); rows.add(row);
        }
        return new PageResult<>(rows,total == null ? 0 : total);
    }

    /** Execution-user summary deliberately omits account, token, reasons and audit identities. */
    public Map<String,Object> status(Long ontologyId, Long bindingId) {
        OntologyDO ontology = access.ontology(ontologyId); bindingOwner(ontologyId,bindingId);
        Map<String,Object> record = approval(bindingId);
        List<Map<String,Object>> sources = jdbc.queryForList(SOURCE_SQL,bindingId,ontologyId);
        String identity = null;
        if (sources.size()==1) {
            try { Map<String,Object> source=sources.get(0); String user=username(source); validateCandidate(source,user); identity=token(source,ontology.getSpaceId(),user); }
            catch (ServiceException | IllegalArgumentException ex) { identity=null; }
        }
        String state = state(record,identity,ontology);
        Map<String,Object> result=protections(); result.put("bindingId",bindingId); result.put("status",state);
        result.put("available",policy.isEnabled() && "APPROVED".equals(state)); result.put("notice",notice(state)); return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public void approve(MetricSourceApprovalSaveReqVO request) { change(request,true); }

    @Transactional(rollbackFor = Exception.class)
    public void revoke(MetricSourceApprovalSaveReqVO request) { change(request,false); }

    private void change(MetricSourceApprovalSaveReqVO r, boolean approve) {
        require(r != null && r.getOntologyId()!=null && r.getBindingId()!=null, "必须指定本体和来源绑定");
        require(r.getRevision()!=null && r.getRevision()>=0 && r.getRevision()<Integer.MAX_VALUE,"来源配置revision无效");
        require(r.getReason()!=null && !r.getReason().isBlank() && r.getReason().length()<=500,"请填写不超过500字的操作说明");
        require(r.getSourceToken()!=null && r.getSourceToken().matches("[a-f0-9]{64}"),"缺少来源确认信息，请刷新");
        OntologyDO ontology=manager(r.getOntologyId());
        require(metadataLocks.lockOntology(ontology.getId()) != null,"本体不存在");
        ontology=manager(r.getOntologyId()); bindingOwner(ontology.getId(),r.getBindingId());
        // Same row lock order for all writers. Sources cannot change while this approval is committed.
        List<Map<String,Object>> bindings=jdbc.queryForList("SELECT id,datasource_id FROM ont_concept_table WHERE id=? AND del_flag=0 FOR UPDATE",r.getBindingId());
        require(bindings.size()==1,"来源绑定不存在");
        jdbc.queryForList("SELECT id FROM ast_datasource WHERE id=? FOR UPDATE",number(bindings.get(0),"datasource_id"));
        Map<String,Object> record=approval(r.getBindingId());
        int current=record==null ? 0 : integer(record,"revision");
        require(current==r.getRevision(),"来源配置已被修改，请刷新后重试");
        int next=current+1; Long actor=SecurityUtils.getUserId();
        if (approve) {
            require(Boolean.TRUE.equals(r.getReadonlyConfirmed()),"请明确确认该账号已由数据库管理员配置为所需只读权限");
            Map<String,Object> source=source(ontology.getId(),r.getBindingId());
            String user=username(source); validateCandidate(source,user);
            String identity=token(source,ontology.getSpaceId(),user);
            require(identity.equals(r.getSourceToken()),"来源已变化，请刷新并重新确认");
            require(hasSpace(number(source,"datasource_id"),ontology),"来源数据源未有效关联当前空间");
            int changed=jdbc.update("INSERT INTO ont_metric_source_approval(binding_id,ontology_id,space_id,concept_id,datasource_id,binding_auth_revision,datasource_auth_revision,username,database_name,schema_name,table_name,source_token,status,revision,reason,approved_by,approved_at,updated_by,updated_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,'APPROVED',?,?,?,CURRENT_TIMESTAMP,?,CURRENT_TIMESTAMP) ON CONFLICT(binding_id) DO UPDATE SET ontology_id=EXCLUDED.ontology_id,space_id=EXCLUDED.space_id,concept_id=EXCLUDED.concept_id,datasource_id=EXCLUDED.datasource_id,binding_auth_revision=EXCLUDED.binding_auth_revision,datasource_auth_revision=EXCLUDED.datasource_auth_revision,username=EXCLUDED.username,database_name=EXCLUDED.database_name,schema_name=EXCLUDED.schema_name,table_name=EXCLUDED.table_name,source_token=EXCLUDED.source_token,status='APPROVED',revision=EXCLUDED.revision,reason=EXCLUDED.reason,approved_by=EXCLUDED.approved_by,approved_at=CURRENT_TIMESTAMP,updated_by=EXCLUDED.updated_by,updated_at=CURRENT_TIMESTAMP WHERE ont_metric_source_approval.revision=?",
                    r.getBindingId(),ontology.getId(),ontology.getSpaceId(),number(source,"concept_id"),number(source,"datasource_id"),number(source,"binding_auth_revision"),number(source,"datasource_auth_revision"),user,source.get("database_name"),source.get("schema_name"),source.get("table_name"),identity,next,r.getReason().trim(),actor,actor,current);
            require(changed==1,"来源配置已被修改，请刷新");
        } else {
            require(record!=null,"来源尚未批准，无需撤销");
            // Revocation must remain possible even if the datasource is now missing/invalid.
            require(Objects.equals(number(record,"ontology_id"),ontology.getId()) && Objects.equals(number(record,"space_id"),ontology.getSpaceId()),"来源批准不属于当前空间");
            require(r.getSourceToken().equals(String.valueOf(record.get("source_token"))) || currentSourceTokenMatches(r,ontology),"来源已变化，请刷新");
            require(jdbc.update("UPDATE ont_metric_source_approval SET status='REVOKED',revision=?,reason=?,updated_by=?,updated_at=CURRENT_TIMESTAMP WHERE binding_id=? AND revision=?",next,r.getReason().trim(),actor,r.getBindingId(),current)==1,"来源配置已被修改，请刷新");
        }
        Map<String,Object> saved=approval(r.getBindingId());
        jdbc.update("INSERT INTO ont_metric_source_approval_event(binding_id,ontology_id,space_id,action,revision,reason,actor_id,username,datasource_id,database_name,schema_name,table_name) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",r.getBindingId(),ontology.getId(),ontology.getSpaceId(),approve?"APPROVE":"REVOKE",next,r.getReason().trim(),actor,saved.get("username"),saved.get("datasource_id"),saved.get("database_name"),saved.get("schema_name"),saved.get("table_name"));
    }

    private boolean currentSourceTokenMatches(MetricSourceApprovalSaveReqVO r, OntologyDO ontology) {
        List<Map<String,Object>> rows=jdbc.queryForList(SOURCE_SQL,r.getBindingId(),ontology.getId());
        if(rows.size()!=1) return false;
        try { return r.getSourceToken().equals(token(rows.get(0),ontology.getSpaceId(),username(rows.get(0)))); }
        catch (ServiceException | IllegalArgumentException ex) { return false; }
    }

    /** Every call reads current committed records; never serves a cached grant. */
    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED, readOnly = true)
    public ExecutionContext authorize(MetricRespVO snapshot) {
        policy.assertEnabled();
        OntologyDO ontology=access.ontology(snapshot.getOntologyId());
        Map<String,Object> source=source(ontology.getId(),snapshot.getConceptTableId());
        Map<String,Object> published=snapshot.getSourceSnapshot();
        require(published!=null && Objects.equals(number(source,"concept_id"),snapshot.getConceptId())
                && Objects.equals(String.valueOf(source.get("datasource_id")),String.valueOf(published.get("datasourceId")))
                && Objects.equals(source.get("database_name"),published.get("databaseName"))
                && Objects.equals(source.get("schema_name"),published.get("schemaName"))
                && Objects.equals(source.get("table_name"),published.get("tableName")),"当前批准来源与发布快照不一致，请重新审查并发布");
        String user=username(source); validateCandidate(source,user);
        String identity=token(source,ontology.getSpaceId(),user);
        Map<String,Object> record=approval(snapshot.getConceptTableId());
        require("APPROVED".equals(state(record,identity,ontology)),"该来源尚未批准、已撤销或来源已变化，请联系空间管理员在查询来源配置中确认");
        require(hasSpace(number(source,"datasource_id"),ontology),"来源数据源未有效关联当前空间");
        DatasourceRespDTO ds=new DatasourceRespDTO(); ds.setId(number(source,"datasource_id"));
        ds.setDatasourceName(text(source,"datasource_name")); ds.setDatasourceType(text(source,"datasource_type"));
        ds.setIp(text(source,"ip")); ds.setPort(number(source,"port")); ds.setDatasourceConfig(text(source,"datasource_config"));
        ds.setValidFlag(true); ds.setSourceAuthRevision(number(source,"datasource_auth_revision"));
        DbQueryProperty property;
        try { property=new DbQueryProperty(ds.getDatasourceType(),ds.getIp(),ds.getPort(),ds.getDatasourceConfig()); }
        catch(RuntimeException ex) { throw new ServiceException("指标来源数据源配置不可用"); }
        return new ExecutionContext(ontology.getId(),snapshot.getConceptTableId(),integer(record,"revision"),identity,property,ds);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED, readOnly = true)
    public void recheck(ExecutionContext context, MetricRespVO snapshot) {
        require(context!=null && Objects.equals(context.ontologyId,snapshot.getOntologyId()) && Objects.equals(context.bindingId,snapshot.getConceptTableId()),"指标执行来源上下文不一致");
        policy.assertEnabled(); OntologyDO ontology=access.ontology(context.ontologyId);
        Map<String,Object> current=source(context.ontologyId,context.bindingId);
        Map<String,Object> record=approval(context.bindingId);
        String identity=token(current,ontology.getSpaceId(),username(current));
        require(hasSpace(number(current,"datasource_id"),ontology),"来源数据源已解除当前空间关联");
        require(Objects.equals(context.datasource.getId(),number(current,"datasource_id"))
                && Objects.equals(context.datasource.getDatasourceType(),text(current,"datasource_type"))
                && Objects.equals(context.datasource.getIp(),text(current,"ip"))
                && Objects.equals(context.datasource.getPort(),number(current,"port"))
                && Objects.equals(context.datasource.getDatasourceConfig(),text(current,"datasource_config")),"指标来源连接身份已变化");
        require(Objects.equals(context.property.getUsername(),username(current)),"指标来源账号不一致");
        require(context.sourceToken.equals(identity) && "APPROVED".equals(state(record,identity,ontology)) && integer(record,"revision")==context.approvalRevision,"来源批准或连接配置已变化，请重新查询");
    }

    private OntologyDO manager(Long ontologyId) {
        OntologyDO ontology=access.ontology(ontologyId);
        List<Map<String,Object>> spaces=jdbc.queryForList("SELECT manager_id FROM tax_space WHERE id=? AND (del_flag IS NULL OR del_flag='0')",ontology.getSpaceId());
        require(spaces.size()==1,"所属空间不存在或不唯一");
        require(Objects.equals(number(spaces.get(0),"manager_id"),SecurityUtils.getUserId()) || SecurityUtils.hasPermi("*:*:*"),"仅空间管理员或平台管理员可配置指标查询来源");
        return ontology;
    }
    private void bindingOwner(Long ontologyId, Long bindingId) {
        List<Map<String,Object>> rows=jdbc.queryForList("SELECT b.id FROM ont_concept_table b JOIN ont_concept c ON c.id=b.concept_id WHERE b.id=? AND c.ontology_id=? AND b.del_flag=0 AND c.del_flag=0",bindingId,ontologyId);
        require(rows.size()==1,"来源绑定不存在或不属于当前本体");
    }
    private Map<String,Object> source(Long ontologyId, Long bindingId) {
        List<Map<String,Object>> rows=jdbc.queryForList(SOURCE_SQL,bindingId,ontologyId);
        require(rows.size()==1,"来源绑定或数据源不存在、已停用或登记不唯一"); return rows.get(0);
    }
    private Map<String,Object> approval(Long bindingId) {
        List<Map<String,Object>> rows=jdbc.queryForList("SELECT * FROM ont_metric_source_approval WHERE binding_id=?",bindingId);
        require(rows.size()<=1,"来源批准登记不唯一"); return rows.isEmpty()?null:rows.get(0);
    }
    private boolean hasSpace(Long datasourceId, OntologyDO ontology) {
        return datasourceApiService.hasDatasourceSpace(datasourceId,ontology.getSpaceId(),ontology.getSpaceCode());
    }
    private static String username(Map<String,Object> source) {
        try { JSONObject config=JSON.parseObject(text(source,"datasource_config")); require(config!=null,"数据源配置不可用"); return config.getString("username"); }
        catch (RuntimeException ex) { throw new ServiceException("数据源账号配置不可用"); }
    }
    private static void validateCandidate(Map<String,Object> source, String username) {
        require(username!=null && !username.isBlank() && username.length()<=128,"数据源账号未配置或过长");
        MetricSqlDialect.of(text(source,"datasource_type")).validateSource(text(source,"database_name"),text(source,"schema_name"),text(source,"table_name"),false);
        require(number(source,"binding_auth_revision")!=null && number(source,"datasource_auth_revision")!=null,"来源版本不可用，请检查迁移");
    }
    static String token(Map<String,Object> source, Long spaceId, String username) {
        // Only IDs, monotonic revisions and nonsecret metadata. Never hash passwords/configuration.
        List<Object> identity=Arrays.asList(spaceId,number(source,"ontology_id"),number(source,"binding_id"),number(source,"concept_id"),number(source,"datasource_id"),number(source,"binding_auth_revision"),number(source,"datasource_auth_revision"),username,source.get("datasource_type"),source.get("database_name"),source.get("schema_name"),source.get("table_name"));
        try {
            byte[] digest=MessageDigest.getInstance("SHA-256").digest(JSON.toJSONString(identity).getBytes(StandardCharsets.UTF_8));
            StringBuilder result=new StringBuilder(64); for(byte b:digest) result.append(String.format(Locale.ROOT,"%02x",b & 255)); return result.toString();
        } catch(java.security.NoSuchAlgorithmException ex) { throw new IllegalStateException("SHA-256 unavailable",ex); }
    }
    private static String state(Map<String,Object> record,String token,OntologyDO ontology) {
        if(token==null) return "UNAVAILABLE";
        if(record==null) return "UNAPPROVED";
        if(!Objects.equals(number(record,"ontology_id"),ontology.getId()) || !Objects.equals(number(record,"space_id"),ontology.getSpaceId()) || !token.equals(record.get("source_token"))) return "STALE";
        return "APPROVED".equals(record.get("status"))?"APPROVED":"REVOKED";
    }
    private String notice(String state) {
        if(!policy.isEnabled()) return "指标查询已被运维紧急关闭，页面批准不能覆盖该开关";
        switch(state) {
            case "APPROVED": return "来源已批准；每次执行仍检查空间、发布版本、字段治理及JDBC只读，不代表数据库实际权限已验证";
            case "REVOKED": return "来源批准已撤销";
            case "STALE": return "绑定或数据源身份已变化，需重新确认只读来源并批准";
            case "UNAVAILABLE": return "来源已停用、登记不唯一或配置不可用于指标查询";
            default: return "来源尚未批准，请联系空间管理员在查询来源配置中确认";
        }
    }
    private Map<String,Object> protections() {
        Map<String,Object> result=new LinkedHashMap<>(); result.put("enabled",policy.isEnabled()); result.put("timeoutSeconds",policy.getTimeoutSeconds()); result.put("maxConcurrent",policy.getMaxConcurrent()); return result;
    }
    private static void pagination(int page,int size) { require(page>=1 && page<=100000 && size>=1 && size<=100,"分页参数无效"); }
    private static Long number(Map<String,Object> row,String key) { Object value=row.get(key); return value==null?null:((Number)value).longValue(); }
    private static int integer(Map<String,Object> row,String key) { return Math.toIntExact(number(row,key)); }
    private static String text(Map<String,Object> row,String key) { Object value=row.get(key); return value==null?null:String.valueOf(value); }
}
