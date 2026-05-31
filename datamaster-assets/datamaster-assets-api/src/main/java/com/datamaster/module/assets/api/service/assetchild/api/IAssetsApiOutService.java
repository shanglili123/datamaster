

package com.datamaster.module.assets.api.service.assetchild.api;

import javax.servlet.http.HttpServletResponse;
import java.util.Map;

public interface IAssetsApiOutService {

    void executeServiceForwarding(HttpServletResponse response, Long apiId, Map<String, Object> params);
}
