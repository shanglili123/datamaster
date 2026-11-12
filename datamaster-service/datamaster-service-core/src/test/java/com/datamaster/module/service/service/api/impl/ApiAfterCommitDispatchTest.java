package com.datamaster.module.service.service.api.impl;

import com.datamaster.module.service.async.AsyncTask;
import com.datamaster.module.service.dal.dataobject.api.ServiceApiDO;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.lang.reflect.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** No database or async threads; invoke the exact transaction callback contract. */
public class ApiAfterCommitDispatchTest {
    private ServiceApiServiceImpl service;
    private AsyncTask async;
    @Before public void setUp() throws Exception {
        service = new ServiceApiServiceImpl(); async = mock(AsyncTask.class);
        Field field = service.getClass().getDeclaredField("asyncTask"); field.setAccessible(true); field.set(service, async);
        TransactionSynchronizationManager.initSynchronization(); TransactionSynchronizationManager.setActualTransactionActive(true);
    }
    @After public void tearDown() {
        TransactionSynchronizationManager.clearSynchronization(); TransactionSynchronizationManager.setActualTransactionActive(false);
    }
    @Test public void mappingIsNotDispatchedBeforeCommitAndCarriesOriginalRoute() throws Exception {
        ServiceApiDO original = new ServiceApiDO(); original.setId(10L); original.setApiUrl("/old");
        dispatch(original); verifyNoInteractions(async);
        for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) synchronization.afterCommit();
        verify(async).releaseOrCancelDataApi(argThat(map -> "10".equals(map.get("id")) && map.get("api") == original));
    }
    @Test public void rollbackNeverDispatchesRegistration() throws Exception {
        ServiceApiDO original = new ServiceApiDO(); original.setId(10L);
        dispatch(original);
        for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
        verifyNoInteractions(async);
    }
    private void dispatch(ServiceApiDO original) throws Exception {
        Method method = service.getClass().getDeclaredMethod("dispatchMapping", String.class, String.class, ServiceApiDO.class); method.setAccessible(true); method.invoke(service, "10", "1", original);
    }
}
