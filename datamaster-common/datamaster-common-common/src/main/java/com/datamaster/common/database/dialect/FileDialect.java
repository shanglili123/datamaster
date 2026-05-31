

package com.datamaster.common.database.dialect;

import org.springframework.web.multipart.MultipartFile;
import com.datamaster.common.database.constants.DbQueryProperty;
import com.datamaster.common.database.core.FileInfo;

import java.util.List;

/**
 * 数据资产-非结构化方言
 *
 * @author Chaos
 * @date 2025-07-16
 */
public interface FileDialect {

    List<FileInfo> getFiles(DbQueryProperty dbQueryProperty, String path);

    void uploadFile(DbQueryProperty dbQueryProperty, String path, MultipartFile file);

}
