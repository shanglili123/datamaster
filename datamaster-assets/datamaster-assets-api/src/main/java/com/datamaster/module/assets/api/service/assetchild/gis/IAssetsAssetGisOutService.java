

package com.datamaster.module.assets.api.service.assetchild.gis;

import javax.servlet.http.HttpServletResponse;
import java.util.Map;

public interface IAssetsAssetGisOutService {
    void executeServiceForwarding(HttpServletResponse response, Long apiId, Map<String, Object> params);

}
