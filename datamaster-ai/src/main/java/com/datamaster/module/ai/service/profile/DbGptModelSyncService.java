package com.datamaster.module.ai.service.profile;

import com.datamaster.common.exception.ServiceException;
import com.datamaster.module.ai.config.DbGptProperties;
import com.datamaster.module.ai.controller.admin.profile.vo.AiModelProfileRespVO;
import com.datamaster.module.ai.service.dbgpt.DbGptModelManagementClient;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** Persistent small-batch queue. Credentials are loaded only after a committed task is claimed. */
@Service
@EnableScheduling
public class DbGptModelSyncService {
    private final JdbcTemplate jdbc;
    private final AiModelProfileCipher cipher;
    private final DbGptProperties properties;
    private final DbGptModelManagementClient client;
    public DbGptModelSyncService(JdbcTemplate jdbc,AiModelProfileCipher cipher,DbGptProperties properties,DbGptModelManagementClient client) {
        this.jdbc=jdbc; this.cipher=cipher; this.properties=properties; this.client=client;
    }
    public boolean enabled() { return properties.getModelSync().isEnabled(); }
    private String targetId() {
        String namespace=properties.getModelSync().getNamespace();
        if(namespace==null || !namespace.matches("[a-z0-9]{4,24}")) throw new ServiceException("请配置稳定且唯一的DB-GPT同步namespace（4到24位小写字母数字）");
        try {
            byte[] digest=MessageDigest.getInstance("SHA-256").digest((client.target()+"|"+namespace).getBytes(StandardCharsets.UTF_8));
            StringBuilder value=new StringBuilder(); for(byte b:digest) value.append(String.format(Locale.ROOT,"%02x",b & 255)); return value.toString();
        } catch(java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    /** Invoked inside the profile save transaction; never sends an HTTP request. */
    public void enqueue(Long profileId,Long spaceId) {
        if(!enabled()) return;
        String target=targetId();
        List<Map<String,Object>> profiles=jdbc.queryForList("SELECT id,space_id,config_revision,enabled FROM ai_model_profile WHERE id=? AND space_id=?",profileId,spaceId);
        if(profiles.size()!=1) throw new ServiceException("当前空间模型配置不存在");
        Map<String,Object> p=profiles.get(0); long version=number(p,"config_revision");
        String alias="dm_"+properties.getModelSync().getNamespace()+"_s"+spaceId+"_p"+profileId+"_v"+version;
        jdbc.update("INSERT INTO ai_dbgpt_model_sync(profile_id,space_id,config_revision,target_id,remote_alias,status) VALUES(?,?,?,?,?,?) ON CONFLICT(profile_id,config_revision,target_id) DO NOTHING",profileId,spaceId,version,target,alias,Boolean.TRUE.equals(p.get("enabled"))?"PENDING":"DISABLED");
        jdbc.update("UPDATE ai_dbgpt_model_sync s SET status='DISABLED',lease_token=NULL,lease_until=NULL,updated_at=CURRENT_TIMESTAMP FROM ai_model_profile p WHERE s.profile_id=p.id AND p.id=? AND p.space_id=? AND (s.config_revision<>p.config_revision OR p.enabled=FALSE) AND s.status<>'DISABLED'",profileId,spaceId);
    }
    @Transactional
    public void retry(Long profileId,Long spaceId) {
        if(!enabled()) throw new ServiceException("DB-GPT接口同步尚未启用");
        enqueue(profileId,spaceId);
        int count=jdbc.update("UPDATE ai_dbgpt_model_sync s SET status='PENDING',attempts=0,last_error=NULL,next_attempt_at=CURRENT_TIMESTAMP,lease_token=NULL,lease_until=NULL,updated_at=CURRENT_TIMESTAMP FROM ai_model_profile p WHERE s.profile_id=p.id AND s.space_id=p.space_id AND s.config_revision=p.config_revision AND p.id=? AND p.space_id=? AND p.enabled=TRUE AND s.target_id=? AND (s.status<>'SYNCING' OR s.lease_until<CURRENT_TIMESTAMP)",profileId,spaceId,targetId());
        if(count!=1) throw new ServiceException("模型已停用或正在同步，请稍后刷新");
    }
    public void describe(AiModelProfileRespVO profile) {
        profile.setDbgptSyncEnabled(enabled()); profile.setDbgptSyncStatus("NOT_ENABLED");
        if(!enabled()) return;
        try {
            String target=targetId();
            List<Map<String,Object>> rows=jdbc.queryForList("SELECT s.status,s.last_error,s.last_attempt_at,s.remote_alias FROM ai_model_profile p LEFT JOIN ai_dbgpt_model_sync s ON s.profile_id=p.id AND s.config_revision=p.config_revision AND s.target_id=? WHERE p.id=? AND p.space_id=?",target,profile.getId(),profile.getSpaceId());
            if(rows.size()!=1) throw new ServiceException("模型同步状态不可用");
            Map<String,Object> row=rows.get(0); profile.setDbgptSyncStatus(row.get("status")==null?"PENDING":String.valueOf(row.get("status")));
            profile.setDbgptSyncError((String)row.get("last_error"));
            profile.setDbgptLastAttemptAt(row.get("last_attempt_at")==null?null:String.valueOf(row.get("last_attempt_at")));
            profile.setDbgptRemoteAlias((String)row.get("remote_alias"));
        } catch(RuntimeException ignored) { profile.setDbgptSyncStatus("FAILED"); profile.setDbgptSyncError("同步配置或状态存储不可用，请检查迁移及安全接线"); }
    }

    @Scheduled(fixedDelayString="${db-gpt.model-sync.poll-interval-ms:5000}")
    public void tick() {
        if(!enabled()) return;
        try {
            String target=targetId();
            // Bounded recovery scan also restores work that was committed before a process exit.
            List<Map<String,Object>> missing=jdbc.queryForList("SELECT p.id,p.space_id FROM ai_model_profile p WHERE p.enabled=TRUE AND NOT EXISTS(SELECT 1 FROM ai_dbgpt_model_sync s WHERE s.profile_id=p.id AND s.config_revision=p.config_revision AND s.target_id=?) ORDER BY p.id LIMIT 10",target);
            for(Map<String,Object> p:missing) enqueue(number(p,"id"),number(p,"space_id"));
            for(int i=0;i<2;i++) { Map<String,Object> task=claim(target); if(task==null) break; process(task); }
        } catch(RuntimeException ignored) { /* Fail closed. Management responses expose unavailable storage/configuration, never secrets. */ }
    }
    private Map<String,Object> claim(String target) {
        String lease=UUID.randomUUID().toString();
        List<Map<String,Object>> rows=jdbc.queryForList("UPDATE ai_dbgpt_model_sync s SET status='SYNCING',attempts=s.attempts+1,lease_token=?,lease_until=CURRENT_TIMESTAMP+INTERVAL '5 minutes',last_attempt_at=CURRENT_TIMESTAMP,updated_at=CURRENT_TIMESTAMP WHERE s.id=(SELECT q.id FROM ai_dbgpt_model_sync q JOIN ai_model_profile p ON p.id=q.profile_id AND p.space_id=q.space_id AND p.config_revision=q.config_revision WHERE q.target_id=? AND p.enabled=TRUE AND ((q.status='PENDING' AND q.next_attempt_at<=CURRENT_TIMESTAMP) OR (q.status='SYNCING' AND q.lease_until<CURRENT_TIMESTAMP)) ORDER BY q.id LIMIT 1 FOR UPDATE OF q SKIP LOCKED) RETURNING s.*",lease,target);
        return rows.isEmpty()?null:rows.get(0);
    }
    private void process(Map<String,Object> task) {
        try {
            List<Map<String,Object>> profiles=jdbc.queryForList("SELECT model,api_url,api_key_ciphertext FROM ai_model_profile WHERE id=? AND space_id=? AND config_revision=? AND enabled=TRUE",task.get("profile_id"),task.get("space_id"),task.get("config_revision"));
            if(profiles.size()!=1) { complete(task,"DISABLED",null); return; }
            Map<String,Object> p=profiles.get(0);
            String key=cipher.decrypt((String)p.get("api_key_ciphertext"),number(task,"space_id"));
            client.ensure((String)task.get("remote_alias"),(String)p.get("model"),(String)p.get("api_url"),key);
            complete(task,"READY",null);
        } catch(RuntimeException ignored) {
            int max=Math.max(1,Math.min(5,properties.getModelSync().getMaxAttempts()));
            complete(task,number(task,"attempts")>=max?"FAILED":"PENDING","远程注册未就绪；请检查管理鉴权、网络和模型兼容参数后重试");
        }
    }
    private void complete(Map<String,Object> task,String status,String error) {
        jdbc.update("UPDATE ai_dbgpt_model_sync s SET status=?,last_error=?,lease_token=NULL,lease_until=NULL,next_attempt_at=CURRENT_TIMESTAMP+INTERVAL '15 seconds',updated_at=CURRENT_TIMESTAMP WHERE s.id=? AND s.status='SYNCING' AND s.lease_token=? AND EXISTS(SELECT 1 FROM ai_model_profile p WHERE p.id=s.profile_id AND p.space_id=s.space_id AND p.config_revision=s.config_revision AND p.enabled=TRUE)",status,error,task.get("id"),task.get("lease_token"));
    }
    /** Caller must already have validated user membership and canonical space. */
    public String requireReady(Long profileId,Long spaceId) {
        if(!enabled()) throw new ServiceException("DB-GPT接口同步未启用");
        List<Map<String,Object>> rows=jdbc.queryForList("SELECT s.remote_alias,s.status,s.id FROM ai_model_profile p JOIN ai_dbgpt_model_sync s ON s.profile_id=p.id AND s.space_id=p.space_id AND s.config_revision=p.config_revision AND s.target_id=? WHERE p.id=? AND p.space_id=? AND p.enabled=TRUE",targetId(),profileId,spaceId);
        if(rows.size()!=1 || !"READY".equals(rows.get(0).get("status"))) throw new ServiceException("当前空间模型尚未同步就绪，请在模型配置页查看状态或重试");
        Map<String,Object> task=rows.get(0); String alias=(String)task.get("remote_alias");
        if(!client.healthy(alias)) {
            jdbc.update("UPDATE ai_dbgpt_model_sync SET status='PENDING',attempts=0,next_attempt_at=CURRENT_TIMESTAMP,last_error='远程模型离线，等待重新注册',updated_at=CURRENT_TIMESTAMP WHERE id=? AND status='READY'",task.get("id"));
            throw new ServiceException("DB-GPT模型已离线，正在重新同步，请稍后重试");
        }
        Long stillCurrent=jdbc.queryForObject("SELECT count(*) FROM ai_model_profile p JOIN ai_dbgpt_model_sync s ON s.profile_id=p.id AND s.space_id=p.space_id AND s.config_revision=p.config_revision WHERE s.id=? AND p.id=? AND p.space_id=? AND p.enabled=TRUE AND s.status='READY'",Long.class,task.get("id"),profileId,spaceId);
        if(stillCurrent==null || stillCurrent!=1) throw new ServiceException("模型配置在健康核对期间已变化，请重试");
        return alias;
    }
    private static long number(Map<String,Object> row,String key) { return ((Number)row.get(key)).longValue(); }
}
