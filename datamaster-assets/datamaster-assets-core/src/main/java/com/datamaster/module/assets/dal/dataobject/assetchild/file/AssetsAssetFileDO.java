package com.datamaster.module.assets.dal.dataobject.assetchild.file;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import com.datamaster.common.core.domain.BaseEntity;
import com.datamaster.common.database.core.FileInfo;

import java.util.Date;

/**
 *  DO  AST_ASSET_FILE
 *
 * @author Chaos
 * @date 2025-07-16
 */
@Data
@TableName(value = "AST_ASSET_FILE")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AssetsAssetFileDO extends BaseEntity {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    /** id */
    private Long assetId;

    private String fileSource;

    private String fileName;

    private String fileUrl;

    private String fileType;

    private Long fileSize;

    private Date fileCreateTime;

    private Date fileUpdateTime;

    private Boolean validFlag;

    @TableLogic
    private Boolean delFlag;

    private String description;

    public FileInfo toFileInfo() {
        FileInfo fileInfo = new FileInfo();
        fileInfo.setDirectory(false);
        fileInfo.setName(fileName);
        fileInfo.setLastModified(fileUpdateTime);
        fileInfo.setPath(fileUrl);
        fileInfo.setSize(fileSize);
        fileInfo.setType(fileType);
        return fileInfo;
    }
}
