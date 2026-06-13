

package com.datamaster.common.utils.uuid;

import java.security.SecureRandom;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ID生成器工具类
 *
 * @author DATAMASTER
 */
public class IdUtils
{
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final AtomicLong SEQ = new AtomicLong(0);

    /**
     * 获取随机UUID
     *
     * @return 随机UUID
     */
    public static String randomUUID()
    {
        return UUID.randomUUID().toString();
    }

    /**
     * 简化的UUID，去掉了横线
     *
     * @return 简化的UUID，去掉了横线
     */
    public static String simpleUUID()
    {
        return UUID.randomUUID().toString(true);
    }

    /**
     * 获取随机UUID，使用性能更好的ThreadLocalRandom生成UUID
     *
     * @return 随机UUID
     */
    public static String fastUUID()
    {
        return UUID.fastUUID().toString();
    }

    /**
     * 简化的UUID，去掉了横线，使用性能更好的ThreadLocalRandom生成UUID
     *
     * @return 简化的UUID，去掉了横线
     */
    public static String fastSimpleUUID()
    {
        return UUID.fastUUID().toString(true);
    }



    /**
     * 生成一个长整型的人造ID，组合当前时间戳和随机数部分，确保较高的唯一性
     *
     * @return long类型的人工ID
     */
    public static long generateArtificialId() {
        long timestamp = System.currentTimeMillis();
        long seq = Math.abs(SEQ.getAndIncrement() % 1000);
        return timestamp * 1000 + seq;
    }
}
