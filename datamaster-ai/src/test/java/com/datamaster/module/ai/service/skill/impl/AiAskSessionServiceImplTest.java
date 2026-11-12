package com.datamaster.module.ai.service.skill.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.datamaster.common.api.space.ISpaceApi;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.module.ai.controller.admin.skill.vo.AiAskSessionRespVO;
import com.datamaster.module.ai.controller.admin.skill.vo.AiAskSessionSaveReqVO;
import com.datamaster.module.ai.dal.dataobject.skill.AiAskSessionDO;
import com.datamaster.module.ai.dal.mapper.skill.AiAskSessionMapper;
import com.datamaster.module.ontology.dal.dataobject.OntologyDO;
import com.datamaster.module.ontology.dal.mapper.OntologyMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.Collections;

import static org.junit.Assert.*;

/** Focused JUnit4 contracts; no database, Spring context, or model calls. */
public class AiAskSessionServiceImplTest {
    private AiAskSessionServiceImpl service;
    private AiAskSessionDO stored;
    private OntologyDO ontology;
    private LambdaUpdateWrapper<AiAskSessionDO> update;
    private boolean member;
    private int writes;

    @Before public void setUp() throws Exception {
        member = true;
        writes = 0;
        stored = new AiAskSessionDO();
        stored.setId(100L);
        stored.setUserId(7L);
        stored.setSpaceId(10L);
        stored.setSpaceCode("space10");
        stored.setTitle("Saved");
        stored.setOntologyId(20L);
        stored.setDelFlag(false);
        ontology = new OntologyDO();
        ontology.setSpaceId(10L);
        ontology.setSpaceCode("space10");
        ontology.setDelFlag(0);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), AiAskSessionDO.class);
        service = new AiAskSessionServiceImpl();
        inject("spaceApi", proxy(ISpaceApi.class, (p, method, args) -> {
            if ("getSpaceCodeBySpaceId".equals(method.getName())) return "space" + args[0];
            if ("hasUserSpace".equals(method.getName())) return member;
            return null;
        }));
        inject("ontologyMapper", proxy(OntologyMapper.class, (p, method, args) -> ontology));
        inject("sessionMapper", proxy(AiAskSessionMapper.class, (p, method, args) -> {
            switch (method.getName()) {
                case "selectById": return stored;
                case "selectRecent": return Collections.singletonList(stored);
                case "insert": stored = (AiAskSessionDO) args[0]; writes++; return 1;
                case "update":
                    @SuppressWarnings("unchecked")
                    LambdaUpdateWrapper<AiAskSessionDO> wrapper = (LambdaUpdateWrapper<AiAskSessionDO>) args[1];
                    update = wrapper; writes++; return 1;
                default: return null;
            }
        }));
        inject("jdbcTemplate", new JdbcTemplate() {
            @Override public <T> T queryForObject(String sql, Class<T> requiredType, Object... args) {
                return requiredType.cast(1);
            }
            @Override public <T> T queryForObject(String sql, Class<T> requiredType) {
                return requiredType.cast(1);
            }
            @Override public int update(String sql) { return 0; }
            @Override public void execute(String sql) { fail("Existing tables must not receive DDL"); }
        });
    }

    @Test public void createAndRestoreOntologyIncludingLegacyNull() {
        AiAskSessionSaveReqVO req = request();
        req.setOntologyId(20L);
        AiAskSessionRespVO created = service.create(7L, "user", req);
        assertEquals(Long.valueOf(20), created.getOntologyId());
        assertEquals("space10", created.getSpaceCode());
        assertEquals(Long.valueOf(20), service.listRecent(7L, 10L, 10).get(0).getOntologyId());
        stored.setOntologyId(null);
        assertNull(service.listRecent(7L, 10L, 10).get(0).getOntologyId());
    }

    @Test public void explicitNullIsWrittenRatherThanSkipped() {
        service.update(7L, "user", 100L, request());
        assertNotNull(update);
        assertTrue(update.getSqlSet().toLowerCase().contains("ontology_id="));
        assertTrue(update.getParamNameValuePairs().containsValue(null));
        assertEquals(1, writes);
    }

    @Test public void rejectsUnauthorizedSpaceAndMismatchingSpaceCode() {
        member = false;
        rejected(() -> service.create(7L, "user", request()));
        rejected(() -> service.listRecent(7L, 10L, 10));
        member = true;
        AiAskSessionSaveReqVO req = request(); req.setSpaceCode("wrong");
        rejected(() -> service.create(7L, "user", req));
        assertEquals(0, writes);
    }

    @Test public void rejectsDeletedMissingOrCrossSpaceOntology() {
        AiAskSessionSaveReqVO req = request(); req.setOntologyId(20L);
        ontology.setDelFlag(1);
        rejected(() -> service.create(7L, "user", req));
        ontology.setDelFlag(0); ontology.setSpaceId(11L);
        rejected(() -> service.create(7L, "user", req));
        ontology.setSpaceId(10L); ontology.setSpaceCode("wrong");
        rejected(() -> service.create(7L, "user", req));
        ontology = null;
        rejected(() -> service.create(7L, "user", req));
        assertEquals(0, writes);
    }

    @Test public void sessionCannotMoveSpaceOrLoseSpaceOrChangeOwner() {
        AiAskSessionSaveReqVO req = request(); req.setSpaceId(11L);
        rejected(() -> service.update(7L, "user", 100L, req));
        req.setSpaceId(null);
        rejected(() -> service.update(7L, "user", 100L, req));
        rejected(() -> service.update(8L, "other", 100L, request()));
        stored.setDelFlag(true);
        rejected(() -> service.update(7L, "user", 100L, request()));
        assertEquals(0, writes);
    }

    private AiAskSessionSaveReqVO request() {
        AiAskSessionSaveReqVO req = new AiAskSessionSaveReqVO();
        req.setSpaceId(10L);
        return req;
    }
    private void inject(String name, Object value) throws Exception {
        Field field = AiAskSessionServiceImpl.class.getDeclaredField(name);
        field.setAccessible(true); field.set(service, value);
    }
    private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
    }
    private static void rejected(Runnable operation) {
        try { operation.run(); fail("Expected safe rejection"); }
        catch (ServiceException expected) { assertNotNull(expected.getMessage()); }
    }
}
