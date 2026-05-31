package com.datamaster.module.system.service.home;

import java.util.List;
import java.util.Map;

public interface ISysHomeService {

    Map<String, Object> getHomeStats(Long projectId, String projectCode);
}
