package com.datamaster.module.ontology.service.analysis;

import com.datamaster.common.api.space.ISpaceApi;
import com.datamaster.common.core.domain.entity.SysUser;
import com.datamaster.common.core.domain.model.LoginUser;
import com.datamaster.module.ontology.dal.dataobject.OntologyDO;
import com.datamaster.module.ontology.dal.mapper.OntologyMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.lang.reflect.Field;
import java.util.Set;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static com.datamaster.module.ontology.service.analysis.MetricQueryBuilderTest.rejected;

public class MetricAnalysisSpaceContextTest {
    private MetricAnalysisAccessGuard guard;
    private OntologyMapper mapper;
    private ISpaceApi spaces;
    @Before public void setUp() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(new LoginUser(7L, 1L, new SysUser(), Set.of()), null));
        guard = new MetricAnalysisAccessGuard(); mapper = mock(OntologyMapper.class); spaces = mock(ISpaceApi.class);
        inject(guard, "ontologyMapper", mapper); inject(guard, "spaceApi", spaces);
        when(spaces.getSpaceCodeBySpaceId(1L)).thenReturn("space01"); when(spaces.getSpaceIdBySpaceCode("space01")).thenReturn(1L);
        when(spaces.hasUserSpace(7L, 1L, "space01")).thenReturn(true);
    }
    @After public void tearDown() { SecurityContextHolder.clearContext(); }
    @Test public void normalizesCodeOnlyStoredSpaceBeforeAnyMetadataAdmission() {
        OntologyDO ontology = new OntologyDO(); ontology.setId(20L); ontology.setSpaceCode("space01"); when(mapper.selectById(20L)).thenReturn(ontology);
        assertEquals(Long.valueOf(1), guard.ontology(20L).getSpaceId());
        verify(spaces).hasUserSpace(7L, 1L, "space01");
    }
    @Test public void missingMismatchAndNonmemberAreRejectedWithoutFallback() {
        OntologyDO ontology = new OntologyDO(); ontology.setId(20L); when(mapper.selectById(20L)).thenReturn(ontology);
        rejected(() -> guard.ontology(20L)); ontology.setSpaceId(1L); ontology.setSpaceCode("other"); rejected(() -> guard.ontology(20L));
        ontology.setSpaceCode("space01"); when(spaces.hasUserSpace(7L, 1L, "space01")).thenReturn(false); rejected(() -> guard.ontology(20L));
    }
    private static void inject(Object bean, String name, Object value) throws Exception { Field field = bean.getClass().getDeclaredField(name); field.setAccessible(true); field.set(bean, value); }
}
