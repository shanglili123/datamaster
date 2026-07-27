package com.datamaster.file.recorder;

import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.recorder.FileRecorder;
import org.dromara.x.file.storage.core.upload.FilePartInfo;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Provides the FileRecorder bean required by x-file-storage.
 */
@Component
public class DataMasterFileRecorder implements FileRecorder {

    private final Map<String, FileInfo> fileInfoMap = new ConcurrentHashMap<>();

    @Override
    public boolean save(FileInfo fileInfo) {
        put(fileInfo);
        return true;
    }

    @Override
    public void update(FileInfo fileInfo) {
        put(fileInfo);
    }

    @Override
    public FileInfo getByUrl(String url) {
        return url == null ? null : fileInfoMap.get(url);
    }

    @Override
    public boolean delete(String url) {
        return url != null && fileInfoMap.remove(url) != null;
    }

    @Override
    public void saveFilePart(FilePartInfo filePartInfo) {
        // Multipart upload metadata is not persisted in DataMaster yet.
    }

    @Override
    public void deleteFilePartByUploadId(String uploadId) {
        // Multipart upload metadata is not persisted in DataMaster yet.
    }

    private void put(FileInfo fileInfo) {
        if (fileInfo == null || fileInfo.getUrl() == null) {
            return;
        }
        fileInfoMap.put(fileInfo.getUrl(), fileInfo);
        if (fileInfo.getThUrl() != null) {
            fileInfoMap.put(fileInfo.getThUrl(), fileInfo);
        }
    }
}
