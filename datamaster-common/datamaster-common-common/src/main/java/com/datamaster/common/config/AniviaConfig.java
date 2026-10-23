

package com.datamaster.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

/**
 * 读取空间相关配置
 *
 * @author DATAMASTER
 */
@Component
@ConfigurationProperties(prefix = "datamaster")
public class AniviaConfig
{
    /** 空间名称 */
    private String name;

    /** 上传路径 */
    private static String profile;

    /** 获取地址开关 */
    private static boolean addressEnabled;

    /** 验证码类型 */
    private static String captchaType;

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getVersion()
    {
        String version = AniviaConfig.class.getPackage().getImplementationVersion();
        return version == null ? "unknown" : version;
    }

    public static String getProfile()
    {
        return profile;
    }

    @Value("${file.storage.root-path}")
    public void setProfile(String profile)
    {
        AniviaConfig.profile = profile;
    }

    public static boolean isAddressEnabled()
    {
        return addressEnabled;
    }

    @Value("${sys.address-enabled:false}")
    public void setAddressEnabled(boolean addressEnabled)
    {
        AniviaConfig.addressEnabled = addressEnabled;
    }

    public static String getCaptchaType() {
        return captchaType;
    }

    @Value("${sys.captcha-type:math}")
    public void setCaptchaType(String captchaType) {
        AniviaConfig.captchaType = captchaType;
    }

    /**
     * 获取导入上传路径
     */
    public static String getImportPath()
    {
        return getProfile() + "/import";
    }

    /**
     * 获取头像上传路径
     */
    public static String getAvatarPath()
    {
        return getProfile() + "/avatar";
    }

    /**
     * 获取下载路径
     */
    public static String getDownloadPath()
    {
        return getProfile() + "/download/";
    }

    /**
     * 获取上传路径
     */
    public static String getUploadPath()
    {
        return getProfile() + "/upload";
    }
}
