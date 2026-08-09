package com.datamaster.module.assets.utils.video;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.datamaster.common.utils.JSONUtils;
import com.datamaster.common.utils.StringUtils;
import com.hikvision.artemis.sdk.ArtemisHttpUtil;
import com.hikvision.artemis.sdk.config.ArtemisConfig;
import org.apache.commons.collections.MapUtils;
import org.springframework.stereotype.Component;
import com.datamaster.module.assets.dal.dataobject.assetchild.video.AssetsAssetVideoDO;

import java.util.HashMap;
import java.util.Map;

@Component
public class VideoHandler {

    /**
     *
     *
     * @return
     * @throws Exception
     */
    private static JSONArray queryVideoList(AssetsAssetVideoDO AssetsAssetVideoDO) throws Exception {
//
        Map<String, Object> objectMap = JSONUtils.convertTaskDefinitionJsonMap(AssetsAssetVideoDO.getConfig());
//
        String appkey = MapUtils.getString(objectMap, "appkey");
//
        String appSecret = MapUtils.getString(objectMap, "appSecret");
//
        String artemisPath = MapUtils.getString(objectMap, "artemisPath");
//
        String ip = AssetsAssetVideoDO.getIp();
//
        Long port = AssetsAssetVideoDO.getPort();
//
        String host = ip + String.valueOf(port);
//
//        /**
//         * https://ip:port/artemis/api/resource/v1/regions
//         * 过查阅AI Cloud开放平台文档或网关门户的文档可以看到分页获取区域列表的定义,这是一个POST请求的Rest接口, 入参为JSON字符串，接口协议为https。
//         * ArtemisHttpUtil工具类提供了doPostStringArtemis调用POST请求的方法，入参可传JSON字符串, 请阅读开发指南了解方法入参，没有的参数可传null
//         */
//
        ArtemisConfig config = new ArtemisConfig();
//
        config.setHost(host); // 代理API网关nginx服务器ip端口
//
        config.setAppKey(appkey);  // 秘钥appkey
//
        config.setAppSecret(appSecret);// 秘钥appSecret
//
        final String getCamsApi = artemisPath + "/api/nms/v1/online/camera/get";
//
        Map<String, String> paramMap = new HashMap<String, String>();// post请求Form表单参数
//
        paramMap.put("pageNo", "1");
//
        paramMap.put("pageSize", "999999");
//
        paramMap.put("treeCode", "0");
//
        String body = JSON.toJSON(paramMap).toString();
//
        Map<String, String> path = new HashMap<String, String>(2) {
            //
            {
//
                put("https://", getCamsApi);
//
            }
//
        };
//
        JSONArray dataList = null;
//
        ArtemisHttpUtil.doPostStringArtemis(config, path, body, null, null, "application/json");

        String resStr = ArtemisHttpUtil.doPostStringArtemis(config, path, body, null, null, "application/json");
//
        if (StringUtils.isNotBlank(resStr)) {
//
            JSONObject res = JSONObject.parseObject(resStr);
//
            if (StringUtils.equals("0", res.getString("code"))) {
//
                dataList = res.getJSONObject("Data").getJSONArray("list");
//
            }
//
        }
//
        return dataList;
    }
}
