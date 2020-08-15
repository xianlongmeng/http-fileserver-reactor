package com.ly.rhdfs.file.config;

import java.io.File;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.ly.common.domain.file.DirectInfo;
import com.ly.common.domain.file.FileInfo;
import com.ly.common.util.DfsFileUtils;

@Service
public class FileInfoManager {

    @Cacheable(value = "file.info", key = "#path")
    public FileInfo findFileInfo(String path) {
        return DfsFileUtils.JSONReadFileInfo(path);
    }

    @CachePut(value = "file.info", key = "#path")
    public FileInfo submitFileInfo(String path, FileInfo fileInfo) {
        if (fileInfo == null || StringUtils.isEmpty(path))
            return null;
        DfsFileUtils.JSONWriteFile(path, fileInfo);
        clearDirectCache(StringUtils.cleanPath(fileInfo.getPath()));
        return fileInfo;
    }

    @CachePut(value = "file.info", key = "#path")
    public FileInfo submitFileInfo(String path, FileInfo fileInfo, String tmpPath) {
        if (fileInfo == null || StringUtils.isEmpty(path))
            return null;
        File file = new File(tmpPath);
        if (!file.exists() || !DfsFileUtils.renameFile(tmpPath, path)) {
            DfsFileUtils.JSONWriteFile(path, fileInfo);
        }
        clearDirectCache(StringUtils.cleanPath(fileInfo.getPath()));
        return fileInfo;
    }

    @Cacheable(value = "direct.info", key = "#path")
    public DirectInfo findDirectInfo(String path) {
        File directFile = new File(path);
        if (!directFile.exists() || !directFile.isDirectory())
            return null;
        DirectInfo directInfo = new DirectInfo();
        directInfo.setItemInfos(DfsFileUtils.findDirectInfoItem(path, false));
        return directInfo;
    }
    @CacheEvict(value = "direct.info",key = "#path")
    public void clearDirectCache(String path){

    }
}
