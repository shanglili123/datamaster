package com.datamaster.module.governance.service.desensitizeHelper;

import lombok.Builder;
import lombok.Data;

/**
 * 脱敏上下文 — 调用方传入当前用户和场景信息
 *
 * @author DATAMASTER
 */
@Data
@Builder
public class DesensitizeContext {

    /** 脱敏场景: "1"=数据资产, "2"=数据查询, "3"=数据服务 */
    private String scene;

    /** 当前用户ID */
    private Long userId;

    /** 用户数据权限等级（数字越小权限越高） */
    private Long userPermissionLevel;
}
