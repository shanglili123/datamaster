package com.datamaster.module.ai.service.profile;

import com.datamaster.common.exception.ServiceException;
import com.datamaster.module.ai.config.DbGptProperties;
import com.datamaster.module.ai.service.dbgpt.DbGptModelManagementClient;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/** Application-JDBC double only. No migration, network or model inference. */
public class DbGptModelSyncServiceTest {
    @Test public void savingOnlyPersistsWorkAndNeverExportsCredentialsBeforeCommit() {
        Fixture f=new Fixture(); f.service.enqueue(2L,1L);
        assertTrue(f.jdbc.sql.stream().anyMatch(sql -> sql.startsWith("INSERT INTO ai_dbgpt_model_sync")));
        assertEquals("dm_test_s1_p2_v1",f.jdbc.alias);
        verify(f.client,never()).ensure(anyString(),anyString(),anyString(),anyString());
        assertFalse(f.jdbc.sql.toString().contains("synthetic-only"));
        f.jdbc.version=2; f.service.enqueue(2L,1L); assertEquals("dm_test_s1_p2_v2",f.jdbc.alias);
    }
    @Test public void disabledFeatureDoesNotReadStorageOrExportKeys() {
        Fixture f=new Fixture(); f.config.getModelSync().setEnabled(false);
        f.service.enqueue(2L,1L); f.service.tick(); assertTrue(f.jdbc.sql.isEmpty()); verifyNoInteractions(f.client);
    }
    @Test public void workerUsesCommittedVersionAndLeaseBoundCompletion() {
        Fixture f=new Fixture(); f.jdbc.claim=true;
        f.service.tick();
        verify(f.client).ensure("dm_test_s1_p2_v1","real-model","https://provider.invalid/v1","synthetic-only");
        assertEquals("READY",f.jdbc.completion);
        String update=f.jdbc.sql.stream().filter(sql -> sql.startsWith("UPDATE ai_dbgpt_model_sync s SET status=?")).findFirst().orElseThrow();
        assertTrue(update.contains("s.lease_token=?")); assertTrue(update.contains("p.config_revision=s.config_revision"));
    }
    @Test public void registrationFailureIsRetriableAndDoesNotPersistRawRemoteError() {
        Fixture f=new Fixture(); f.jdbc.claim=true;
        doThrow(new ServiceException("SECRET provider key raw-body")).when(f.client).ensure(anyString(),anyString(),anyString(),anyString());
        f.service.tick(); assertEquals("PENDING",f.jdbc.completion);
        assertNotNull(f.jdbc.error); assertFalse(f.jdbc.error.contains("SECRET"));
    }
    @Test public void remoteRestartQueuesRecoveryAndRejectsCurrentRequest() {
        Fixture f=new Fixture(); when(f.client.healthy("dm_test_s1_p2_v1")).thenReturn(false);
        try { f.service.requireReady(2L,1L); fail("Must reject offline model"); } catch(ServiceException expected) { assertTrue(expected.getMessage().contains("离线")); }
        assertTrue(f.jdbc.sql.stream().anyMatch(sql -> sql.contains("远程模型离线")));
    }
    private static class Fixture {
        final DbGptProperties config=new DbGptProperties();
        final AiModelProfileCipher cipher=new AiModelProfileCipher(Base64.getEncoder().encodeToString(new byte[16]));
        final MemoryJdbc jdbc=new MemoryJdbc();
        final DbGptModelManagementClient client=mock(DbGptModelManagementClient.class);
        final DbGptModelSyncService service;
        Fixture() {
            config.getModelSync().setEnabled(true); config.getModelSync().setNamespace("test");
            when(client.target()).thenReturn("https://dbgpt.invalid"); jdbc.encrypted=cipher.encrypt("synthetic-only",1L);
            service=new DbGptModelSyncService(jdbc,cipher,config,client);
        }
    }
    private static class MemoryJdbc extends JdbcTemplate {
        final List<String> sql=new ArrayList<>(); String alias; long version=1; String encrypted; boolean claim; String completion,error;
        @Override public List<Map<String,Object>> queryForList(String query,Object...args) {
            sql.add(query);
            if(query.startsWith("SELECT id,space_id,config_revision")) return List.of(Map.of("id",2L,"space_id",1L,"config_revision",version,"enabled",true));
            if(query.startsWith("SELECT p.id,p.space_id")) return List.of();
            if(query.startsWith("UPDATE ai_dbgpt_model_sync s SET status='SYNCING'")) {
                if(!claim) return List.of(); claim=false;
                Map<String,Object> task=new HashMap<>(); task.put("id",9L); task.put("profile_id",2L); task.put("space_id",1L); task.put("config_revision",1L);
                task.put("remote_alias","dm_test_s1_p2_v1"); task.put("attempts",1L); task.put("lease_token",args[0]); return List.of(task);
            }
            if(query.startsWith("SELECT model,api_url")) return List.of(Map.of("model","real-model","api_url","https://provider.invalid/v1","api_key_ciphertext",encrypted));
            if(query.startsWith("SELECT s.remote_alias,s.status")) return List.of(Map.of("id",9L,"remote_alias","dm_test_s1_p2_v1","status","READY"));
            throw new AssertionError("Unexpected query: "+query);
        }
        @Override public int update(String query,Object...args) {
            sql.add(query);
            if(query.startsWith("INSERT INTO ai_dbgpt_model_sync")) alias=(String)args[4];
            if(query.startsWith("UPDATE ai_dbgpt_model_sync s SET status=?")) { completion=(String)args[0]; error=(String)args[1]; }
            return 1;
        }
    }
}
