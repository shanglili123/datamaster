package com.datamaster.module.collector.service.etl.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlTaskDO;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlTaskExtDO;
import com.datamaster.module.collector.dal.mapper.etl.CollectorEtlTaskMapper;
import com.datamaster.module.collector.service.etl.ICollectorEtlTaskExtService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/** 发布版本专用的随机凭据；只校验任务回调，不授予普通登录/API权限。 */
@Service
public class CollectorEtlCallbackTokenService {
    public static final String HEADER = "X-DataMaster-Task-Token";
    @Resource private CollectorEtlTaskMapper taskMapper;
    @Resource private ICollectorEtlTaskExtService extService;

    public static String generate() {
        byte[] random = new byte[32];
        new SecureRandom().nextBytes(random);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(random);
    }

    public static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

    /** 调用方持有任务行锁，确保校验凭据后发布快照不会在本次操作中被替换。 */
    public void requireToken(Long taskId, String supplied) {
        if (supplied == null || !supplied.matches("[A-Za-z0-9_-]{43}")) throw denied();
        CollectorEtlTaskDO task = taskMapper.selectById(taskId);
        CollectorEtlTaskExtDO ext = extService.getByTaskId(taskId);
        if (task == null || !"1".equals(task.getStatus()) || ext == null || ext.getFlinkxJobTemplateJson() == null) throw denied();
        JSONObject snapshot = JSON.parseObject(ext.getFlinkxJobTemplateJson());
        String expected = snapshot.getString("callbackTokenHash");
        if (!CollectorEtlStageRuntimeService.KIND.equals(snapshot.getString("kind"))
                || !java.util.Objects.equals(ext.getEtlTaskCode(), snapshot.getString("workflowCode"))
                || !java.util.Objects.equals(String.valueOf(ext.getEtlTaskVersion()), snapshot.getString("workflowVersion")) || expected == null
                || !MessageDigest.isEqual(expected.getBytes(StandardCharsets.US_ASCII), hash(supplied).getBytes(StandardCharsets.US_ASCII))) {
            throw denied();
        }
    }

    private ServiceException denied() { return new ServiceException("任务回调凭据无效", 403); }
}
