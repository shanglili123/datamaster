package com.datamaster.module.ai.service.profile;

import com.datamaster.common.exception.ServiceException;
import com.datamaster.module.ai.controller.admin.profile.vo.AiModelProfileRespVO;
import com.datamaster.module.ai.controller.admin.profile.vo.AiModelProfileSaveReqVO;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.lang.reflect.Proxy;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class AiModelProfileServiceTest {
    private static final String KEY = Base64.getEncoder().encodeToString(new byte[16]);

    @Test public void authenticatedEncryptionHasNoDefaultAndIsSpaceBound() {
        AiModelProfileCipher cipher = new AiModelProfileCipher(KEY);
        String encrypted = cipher.encrypt("test-credential", 10L);
        assertTrue(encrypted.startsWith("gcm1:"));
        assertFalse(encrypted.contains("test-credential"));
        assertEquals("test-credential", cipher.decrypt(encrypted, 10L));
        assertNotEquals(encrypted, cipher.encrypt("test-credential", 10L));
        rejected(() -> cipher.decrypt(encrypted, 11L));
        rejected(() -> new AiModelProfileCipher("").encrypt("test-credential", 10L));
        rejected(() -> new AiModelProfileCipher("invalid").decrypt(encrypted, 10L));
        rejected(() -> cipher.decrypt("plaintext", 10L));
        char last = encrypted.charAt(encrypted.length() - 3);
        String tampered = encrypted.substring(0, encrypted.length() - 3) + (last == 'A' ? 'B' : 'A') + encrypted.substring(encrypted.length() - 2);
        rejected(() -> cipher.decrypt(tampered, 10L));
    }

    @Test public void endpointAndModelValidationNeverCallsNetwork() {
        AiModelProfileSaveReqVO request = valid();
        AiModelProfileService.validate(request);
        AiModelProfileService.validateUrl("http://localhost:8000/v1/chat/completions");
        for (String url : Arrays.asList("file:///tmp/a", "ftp://host/path", "https://user:secret@host/v1", "https://host/v1?key=secret", "https://host/v1#fragment", "https://host:99999/v1", "https://host/\nsecret"))
            rejected(() -> AiModelProfileService.validateUrl(url));
        request.setProvider("invalid-provider"); rejected(() -> AiModelProfileService.validate(request));
        request.setProvider("DEEPSEEK"); request.setModel("model\nheader"); rejected(() -> AiModelProfileService.validate(request));
        request.setModel("model"); request.setApiKey(String.join("", Collections.nCopies(8193, "x")));
        rejected(() -> AiModelProfileService.validate(request));
    }

    @Test public void blankCredentialUpdatePreservesCiphertextAndLocksSpaceFirst() {
        MemoryJdbc jdbc = new MemoryJdbc();
        AiModelProfileService service = new AiModelProfileService(jdbc, new AiModelProfileCipher(""));
        AiModelProfileSaveReqVO request = valid(); request.setApiKey("  ");
        service.update(7L, 10L, "space10", request);
        assertTrue(jdbc.statements.get(0).contains("tax_space"));
        assertTrue(jdbc.statements.get(0).endsWith("FOR UPDATE"));
        assertFalse(jdbc.statements.stream().anyMatch(sql -> sql.contains("SET api_key_ciphertext")));
        assertFalse(jdbc.statements.stream().anyMatch(sql -> sql.startsWith("SELECT") && sql.contains("SELECT api_key_ciphertext")));
        assertFalse(service.list(10L).get(0).getActive());
        assertTrue(service.list(10L).get(0).getHasApiKey());
    }

    @Test public void activeDisableAndDeleteAreRejectedBeforeWrites() {
        MemoryJdbc jdbc = new MemoryJdbc(); jdbc.active = true;
        AiModelProfileService service = new AiModelProfileService(jdbc, new AiModelProfileCipher(KEY));
        AiModelProfileSaveReqVO request = valid(); request.setEnabled(false);
        rejected(() -> service.update(7L, 10L, "space10", request));
        rejected(() -> service.delete(7L, 10L));
        assertEquals(0, jdbc.writes);
    }

    @Test public void resolverScopesSelectionAndDoesNotFallbackAfterDecryptionFailure() {
        MemoryJdbc jdbc = new MemoryJdbc();
        AiModelProfileService service = new AiModelProfileService(jdbc, new AiModelProfileCipher(KEY));
        assertNull(service.resolve(null, null));
        assertTrue(jdbc.statements.isEmpty());
        jdbc.empty = true;
        assertNull(service.resolve(10L, null));
        rejected(() -> service.resolve(10L, 7L));
        assertTrue(jdbc.statements.get(jdbc.statements.size() - 1).contains("space_id = ? AND id = ?"));
        assertArrayEquals(new Object[]{10L, 7L}, jdbc.lastArgs);
        jdbc.empty = false;
        rejected(() -> service.resolve(10L, 7L)); // invalid ciphertext must never return null/fallback
        jdbc.enabled = false;
        rejected(() -> service.activate(7L, 10L));
        assertEquals(0, jdbc.writes);
    }

    private static AiModelProfileSaveReqVO valid() {
        AiModelProfileSaveReqVO request = new AiModelProfileSaveReqVO();
        request.setName("Primary"); request.setProvider("OPENAI_COMPATIBLE"); request.setModel("provider/model-v1:latest");
        request.setApiUrl("https://example.invalid/v1/chat/completions"); request.setEnabled(true); request.setApiKey("test-credential");
        return request;
    }
    private static void rejected(Runnable action) {
        try { action.run(); fail("Expected safe rejection"); } catch (ServiceException expected) { assertNotNull(expected.getMessage()); }
    }

    private static final class MemoryJdbc extends JdbcTemplate {
        final List<String> statements = new ArrayList<>();
        Object[] lastArgs;
        int writes;
        boolean active;
        boolean enabled = true;
        boolean empty;
        @Override public int update(String sql, Object... args) { statements.add(sql); writes++; return 1; }
        @Override public <T> List<T> query(String sql, RowMapper<T> mapper, Object... args) {
            statements.add(sql); lastArgs = args;
            if (empty) return Collections.emptyList();
            Map<String, Object> values = new HashMap<>();
            values.put("id", 7L); values.put("space_id", 10L); values.put("space_code", "space10");
            values.put("name", "Primary"); values.put("provider", "OPENAI_COMPATIBLE"); values.put("model", "model");
            values.put("api_url", "https://example.invalid/v1"); values.put("active", active);
            values.put("enabled", enabled); values.put("has_api_key", true); values.put("api_key_ciphertext", "not-valid-ciphertext");
            ResultSet rs = (ResultSet) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{ResultSet.class}, (proxy, method, params) -> {
                Object key = params == null ? null : params[0];
                Object value = key instanceof Integer ? 10L : values.get(key);
                if (method.getName().equals("getLong")) return value == null ? 0L : value;
                if (method.getName().equals("getBoolean")) return value != null && (Boolean) value;
                if (method.getName().equals("getString")) return value == null ? null : value.toString();
                return null;
            });
            try { return Collections.singletonList(mapper.mapRow(rs, 0)); }
            catch (java.sql.SQLException ex) { throw new AssertionError(ex); }
        }
    }
}
