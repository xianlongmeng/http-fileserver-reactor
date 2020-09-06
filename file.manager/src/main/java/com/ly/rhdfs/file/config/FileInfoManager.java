package com.ly.rhdfs.file.config;

import java.io.File;

import org.springframework.beans.factory.annotation.Autowired;
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
    private DfsFileUtils dfsFileUtils;
    @Autowired
    private void setDfsFileUtils(DfsFileUtils dfsFileUtils){
        this.dfsFileUtils=dfsFileUtils;
    }
    @Cacheable(value = "file.info", key = "#path")
    public FileInfo findFileInfo(String path) {
        return dfsFileUtils.JSONReadFileInfo(path);
    }

    @CachePut(value = "file.info", key = "#path")
    public FileInfo submitFileInfo(FileInfo fileInfo) {
        if (fileInfo == null)
            return null;
        String path=dfsFileUtils.joinFileConfigName(fileInfo.getPath(),fileInfo.getFileName());
        if ( StringUtils.isEmpty(path))
            return null;
        dfsFileUtils.JSONWriteFile(path, fileInfo);
        clearDirectCache(StringUtils.cleanPath(fileInfo.getPath()));
        return fileInfo;
    }

    @CachePut(value = "file.info", key = "#path")
    public FileInfo submitFileInfo(FileInfo fileInfo, String tmpPath) {
        if (fileInfo == null)
            return null;
        String path=dfsFileUtils.joinFileConfigName(fileInfo.getPath(),fileInfo.getFileName());
        if ( StringUtils.isEmpty(path))
            return null;
        File file = new File(tmpPath);
        if (!file.exists() || !dfsFileUtils.renameFile(tmpPath, path)) {
            dfsFileUtils.JSONWriteFile(path, fileInfo);
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
        directInfo.setItemInfos(dfsFileUtils.findDirectInfoItem(path, false));
        return directInfo;
    }
    @CacheEvict(value = "direct.info",key = "#path")
    public void clearDirectCache(String path){

    }
}
