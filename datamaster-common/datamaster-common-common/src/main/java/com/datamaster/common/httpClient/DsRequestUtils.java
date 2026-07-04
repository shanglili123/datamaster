

package com.datamaster.common.httpClient;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.alibaba.fastjson2.JSON;
import com.datamaster.common.exception.ServiceException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * <P>
 * 用途:调度器请求工具
 * </p>
 *
 * @author: FXB
 * @create: 2025-02-18 14:39
 **/
@Component
public class DsRequestUtils {

    private static String baseUrl;//ds请求接口前缀
    private static String token;//ds令牌
    private static int timeout = 30000;//ds请求超时时间，避免前端发布按钮长时间无响应

    @Value("${ds.token}")
    public void setToken(String token) {
        this.token = token;
    }

    @Value("${ds.base_url}")
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @Value("${ds.timeout:30000}")
    public void setTimeout(int timeout) {
        DsRequestUtils.timeout = timeout;
    }

    /**
     * 请求方法
     *
     * @param url         接口路径
     * @param method      请求方法
     * @param body        body参数
     * @param params      url拼接的参数 map
     * @param resultClass 结果class
     * @return
     */
    public static <T> T request(String url, String method, Object body, Map<String, Object> params, Class<T> resultClass) {
        //拼接url参数
        if (params != null && !params.isEmpty()) {
            String paramsStr = HttpUtil.toParams(params);
            if (url.indexOf("?") > -1) {
                url = url + "&" + paramsStr;
            } else {
                url = url + "?" + paramsStr;
            }
        }

        //封装请求对象
        String requestUrl = baseUrl + url;
        HttpRequest request = HttpUtil.createRequest(Method.valueOf(method), requestUrl)
                .header("token", token)
                .timeout(timeout);
        if (body != null) {
            request.body(JSON.toJSONString(body));
        }
        //获取结果
        HttpResponse response = request.execute();
        return parseResponse(response, requestUrl, resultClass);
    }

    /**
     * 请求方法(表单传参)
     *
     * @param url         接口路径
     * @param method      请求方法
     * @param params      map
     * @param resultClass 结果class
     * @return
     */
    public static <T> T requestForm(String url, String method, Map<String, Object> params, Class<T> resultClass) {
        //拼接url参数（DS API 要求参数在 query string 中）
        if (params != null && !params.isEmpty()) {
            String paramsStr = HttpUtil.toParams(params);
            if (url.indexOf("?") > -1) {
                url = url + "&" + paramsStr;
            } else {
                url = url + "?" + paramsStr;
            }
        }
        //封装请求对象
        String requestUrl = baseUrl + url;
        HttpRequest request = HttpUtil.createRequest(Method.valueOf(method), requestUrl)
                .header("token", token)
                .timeout(timeout);
        //获取结果
        HttpResponse response = request.execute();
        return parseResponse(response, requestUrl, resultClass);
    }

    private static <T> T parseResponse(HttpResponse response, String requestUrl, Class<T> resultClass) {
        String body = response.body();
        if (response.getStatus() < 200 || response.getStatus() >= 300) {
            throw new ServiceException("DolphinScheduler接口请求失败，状态码：" + response.getStatus()
                    + "，地址：" + requestUrl + "，响应：" + abbreviate(body));
        }
        if (StringUtils.isBlank(body) || !isJsonResponse(body)) {
            throw new ServiceException("DolphinScheduler接口返回非JSON响应，地址：" + requestUrl
                    + "，Content-Type：" + response.header("Content-Type")
                    + "，响应：" + abbreviate(body));
        }
        return JSON.parseObject(body, resultClass);
    }

    private static boolean isJsonResponse(String body) {
        String trimBody = StringUtils.trim(body);
        return StringUtils.startsWith(trimBody, "{") || StringUtils.startsWith(trimBody, "[");
    }

    private static String abbreviate(String body) {
        if (body == null) {
            return "";
        }
        String text = StringUtils.normalizeSpace(body);
        return StringUtils.abbreviate(text, 300);
    }

    /**
     * 替换项目编码
     *
     * @param url
     * @param projectCode
     * @return
     */
    public static String replaceProjectCode(String url, String projectCode) {
        return StringUtils.replace(url, "{projectCode}", projectCode);
    }

    /**
     * 替换项目编码及id
     *
     * @param url
     * @param projectCode
     * @param id
     * @return
     */
    public static String replaceProjectCodeAndId(String url, String projectCode, Long id) {
        return StringUtils.replace(StringUtils.replace(url, "{projectCode}", projectCode), "{id}", String.valueOf(id));
    }

    /**
     * 替换项目编码及code
     *
     * @param url
     * @param projectCode
     * @param code
     * @return
     */
    public static String replaceProjectCodeAndCode(String url, String projectCode, String code) {
        return StringUtils.replace(StringUtils.replace(url, "{projectCode}", projectCode), "{code}", String.valueOf(code));
    }
}
