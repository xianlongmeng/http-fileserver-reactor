package com.ly.rhdfs.file.config;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.ly.common.domain.file.FileItemInfo;
import com.ly.common.domain.file.ItemInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.ly.common.domain.file.DirectInfo;
import com.ly.common.domain.file.FileInfo;
import com.ly.rhdfs.file.util.DfsFileUtils;

@Service
public class FileInfoManager {
    private DfsFileUtils dfsFileUtils;
    @Autowired
    private void setDfsFileUtils(DfsFileUtils dfsFileUtils){
        this.dfsFileUtils=dfsFileUtils;
    }
    public FileInfo findFileInfo(String path,String fileName){
        return findFileInfo(dfsFileUtils.joinFileName(path,fileName));
    }
    @Cacheable(value = "file.info", key = "#fileFullName")
    public FileInfo findFileInfo(String fileFullName) {
        return dfsFileUtils.JSONReadFileInfo(dfsFileUtils.joinFileConfigName(fileFullName));
    }

    @Cacheable(value = "file.info", key = "#fileFullName")
    public FileInfo findConfigFileInfo(String fileFullName) {
        return dfsFileUtils.JSONReadConfigFileInfo(fileFullName);
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
        List<ItemInfo> itemInfoList=dfsFileUtils.findDirectInfoItem(path, false);
        List<ItemInfo> itemInfos= new ArrayList<>();
        directInfo.setItemInfos(itemInfos);
        for (ItemInfo itemInfo:itemInfoList){
            if (itemInfo==null)
                continue;
            if (itemInfo.isDirectory())
                itemInfos.add(itemInfo);
            else{
                FileItemInfo fileItemInfo=new FileItemInfo(itemInfo.getName());
                FileInfo fileInfo=findConfigFileInfo(itemInfo.getName());
                if (fileInfo!=null){
                    fileItemInfo.setSize(fileInfo.getSize());
                    fileItemInfo.setLastModifyTime(fileInfo.getLastModifyTime());
                    fileItemInfo.setEtag(fileInfo.getEtag());
                    itemInfos.add(fileItemInfo);
                }
            }
        }
        return directInfo;
    }
    @CacheEvict(value = "direct.info",key = "#path")
    public void clearDirectCache(String path){

    }
    @CacheEvict(value = "file.info", key = "#root.getKey(path,fileName)")
    public Boolean deleteFile(String path,String fileName) {
        return dfsFileUtils.fileDelete(path,fileName);
    }
    private String getKey(String path,String fileName){
        return dfsFileUtils.joinFileName(path,fileName);
    }
}
