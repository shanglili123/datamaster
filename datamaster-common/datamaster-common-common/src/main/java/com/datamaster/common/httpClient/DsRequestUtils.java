

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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
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
        Method httpMethod = Method.valueOf(method);
        String requestUrl = baseUrl + url;
        HttpRequest request;
        // Spring @RequestParam 同时接受查询参数和 URL 编码表单。大任务定义必须放请求体，
        // 否则任务 JSON 会触发 URI 长度限制，并把数据库凭据泄露到 URL/代理日志。
        if (httpMethod == Method.POST || httpMethod == Method.PUT || httpMethod == Method.PATCH) {
            request = HttpUtil.createRequest(httpMethod, requestUrl)
                    .contentType("application/x-www-form-urlencoded; charset=UTF-8")
                    .body(encodeForm(params));
        } else {
            if (params != null && !params.isEmpty()) {
                requestUrl += (requestUrl.contains("?") ? "&" : "?") + HttpUtil.toParams(params);
            }
            request = HttpUtil.createRequest(httpMethod, requestUrl);
        }
        request.header("token", token).timeout(timeout);
        try (HttpResponse response = request.execute()) {
            return parseResponse(response, requestUrl, resultClass);
        }
    }

    public static String requestRaw(String url, String method, Map<String, Object> params) {
        if (params != null && !params.isEmpty()) {
            String paramsStr = HttpUtil.toParams(params);
            if (url.indexOf("?") > -1) {
                url = url + "&" + paramsStr;
            } else {
                url = url + "?" + paramsStr;
            }
        }
        String requestUrl = baseUrl + url;
        HttpRequest request = HttpUtil.createRequest(Method.valueOf(method), requestUrl)
                .header("token", token)
                .timeout(timeout);
        HttpResponse response = request.execute();
        String body = response.body();
        if (response.getStatus() < 200 || response.getStatus() >= 300) {
            throw new ServiceException("DolphinScheduler接口请求失败，状态码：" + response.getStatus()
                    + "，地址：" + safeRequestUrl(requestUrl) + "，响应：" + abbreviate(body));
        }
        return body;
    }

    private static <T> T parseResponse(HttpResponse response, String requestUrl, Class<T> resultClass) {
        String body = response.body();
        if (response.getStatus() < 200 || response.getStatus() >= 300) {
            throw new ServiceException("DolphinScheduler接口请求失败，状态码：" + response.getStatus()
                    + "，地址：" + safeRequestUrl(requestUrl) + "，响应：" + abbreviate(body));
        }
        if (StringUtils.isBlank(body) || !isJsonResponse(body)) {
            throw new ServiceException("DolphinScheduler接口返回非JSON响应，地址：" + safeRequestUrl(requestUrl)
                    + "，Content-Type：" + response.header("Content-Type")
                    + "，响应：" + abbreviate(body));
        }
        return JSON.parseObject(body, resultClass);
    }

    private static boolean isJsonResponse(String body) {
        String trimBody = StringUtils.trim(body);
        return StringUtils.startsWith(trimBody, "{") || StringUtils.startsWith(trimBody, "[");
    }

    private static String encodeForm(Map<String, Object> params) {
        if (params == null || params.isEmpty()) return "";
        StringBuilder result = new StringBuilder();
        try {
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                if (entry.getValue() == null) continue;
                if (result.length() > 0) result.append('&');
                result.append(URLEncoder.encode(entry.getKey(), "UTF-8")).append('=')
                        .append(URLEncoder.encode(String.valueOf(entry.getValue()), "UTF-8"));
            }
        } catch (UnsupportedEncodingException ex) {
            throw new IllegalStateException("UTF-8 编码不可用", ex);
        }
        return result.toString();
    }

    private static String safeRequestUrl(String url) {
        int query = url.indexOf('?');
        return query < 0 ? url : url.substring(0, query) + "?[参数已隐藏]";
    }

    private static String abbreviate(String body) {
        if (body == null) {
            return "";
        }
        // DS/代理可能回显原始请求和任务 JSON，不能把响应正文原样返回到前端或日志。
        String text = StringUtils.normalizeSpace(body);
        if (StringUtils.startsWith(text, "<")) {
            return "[HTML错误响应，正文已隐藏]";
        }
        return "[错误响应正文已隐藏]";
    }

    /**
     * 替换空间编码
     *
     * @param url
     * @param projectCode
     * @return
     */
    public static String replaceProjectCode(String url, String projectCode) {
        return StringUtils.replace(url, "{projectCode}", projectCode);
    }

    /**
     * 替换空间编码及id
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
     * 替换空间编码及code
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
