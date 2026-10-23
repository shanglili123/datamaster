

package com.datamaster.module.governance.api.model.dto;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 逻辑模型 DTO 对象 STD_MODEL
 *
 * @author DATAMASTER
 * @date 2025-01-21
 */
@Data
public class StandardsModelRespDTO {

    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    private Long id;

    /**
     * 模型编码
     */
    private String modelName;

    /**
     * 模型名称
     */
    private String modelComment;

    /**
     * 类目编码
     */
    private String catCode;

    @TableField(exist = false)
    private String catName;

    /**
     * 表类型;1:明细表 2:汇总表 3:维度表 4:应用表
     */
    @Schema(name = "表类型 1:明细表 2:汇总表 3:维度表 4:应用表")
    private String tableType;
    /**
     * 数仓分层id
     */
    @Schema(name = "数仓分层id ")
    private Long dataLayerId;
    /**
     * 可选业务分类标签（所有表类型）；不参与物理表命名
     */
    @Schema(name = "可选业务分类标签，不参与表名")
    private Long businessCategoryId;
    /**
     * 业务分类层级编码
     */
    @Schema(name = "业务分类层级编码 ")
    private String businessCategoryCode;
    /**
     * 数据分域id;只有表类型为非应用表是才有值
     */
    @Schema(name = "数据分域id 只有表类型为非应用表是才有值")
    private Long dataDomainId;
    /**
     * 所属主题id（主题规划）;只有表类型为应用表是才有值
     */
    @Schema(name = "所属主题id（主题规划） 只有表类型为应用表是才有值")
    private Long themeDomainId;
    /**
     * 所属主题层级编码
     */
    @Schema(name = "所属主题层级编码 ")
    private String themeDomainCode;
    /**
     * 表名大小写;1：大写 2：小写
     */
    @Schema(name = "表名大小写 1：大写 2：小写")
    private String tableCase;

    /**
     * 状态
     */
    private String status;

    /**
     * 创建方式
     */
    private String createType;

    /**
     * 数据源id
     */
    private Long datasourceId;

    private Long documentId;

    /**
     * 联系人
     */
    private String contact;

    /**
     * 联系电话
     */
    private String contactNumber;

    /**
     * 描述
     */
    private String description;

    /**
     * 是否有效
     */
    private Boolean validFlag;

    /**
     * 删除标志
     */
    @TableLogic
    private Boolean delFlag;

    @TableField(exist = false)
    private long columnCount;

    /**
     * 数据源名称
     */
    @TableField(exist = false)
    private String datasourceName;

    /**
     * 数据源类型
     */
    @TableField(exist = false)
    private String datasourceType;

    /**
     * 数据源配置(json字符串)
     */
    @TableField(exist = false)
    private String datasourceConfig;

    /**
     * IP
     */
    @TableField(exist = false)
    private String ip;

    /**
     * 端口号
     */
    @TableField(exist = false)
    private Long port;

    /**
     * 名称
     */
    @TableField(exist = false)
    private String documentName;

    /**
     * 名称
     */
    @TableField(exist = false)
    private String documentCode;

    /**
     * 文件标准类型字段，
     */
    @TableField(exist = false)
    private String documentType;

    @Schema(name = "数仓分层名称")
    @TableField(exist = false)
    private String dataLayerName;

    @Schema(name = "数仓分层英文缩写")
    @TableField(exist = false)
    private String dataLayerEngName;

    @Schema(name = "业务名称")
    @TableField(exist = false)
    private String businessCategoryName;

    @Schema(name = "业务英文缩写")
    @TableField(exist = false)
    private String businessCategoryEngName;

    @Schema(name = "数据分域名称")
    @TableField(exist = false)
    private String dataDomainName;

    @Schema(name = "数据分域英文缩写")
    @TableField(exist = false)
    private String dataDomainEngName;

    @Schema(name = "所属主题名称")
    @TableField(exist = false)
    private String themeDomainName;

    @Schema(name = "所属主题英文缩写")
    @TableField(exist = false)
    private String themeDomainEngName;

    @Schema(name = "发布状态 1:未发布 3:发布成功 4:发布失败")
    @TableField(exist = false)
    private String releaseStatus;

    @Schema(name = "发布发布数据源列表")
    @TableField(exist = false, typeHandler = JacksonTypeHandler.class)
    private String releaseDatabaseList;

    @Schema(name = "创建人联系电话")
    @TableField(exist = false, typeHandler = JacksonTypeHandler.class)
    private String createUserPhoneNumber;

    /** Frozen physical target; LEGACY preserves its original model name. */
    public String getTableName() {
        return physicalTableName != null && !physicalTableName.isEmpty() ? physicalTableName : modelName;
    }


    // Warehouse governance and frozen target.
    private String physicalTableName;

    private String namingVersion;

    private String dataLayerCode;

    private String dataDomainCode;

    private Long spaceId;

    private String spaceCode;
}
