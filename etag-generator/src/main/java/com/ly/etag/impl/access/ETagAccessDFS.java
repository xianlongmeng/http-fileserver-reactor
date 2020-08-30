package com.ly.etag.impl.access;

import com.ly.common.domain.file.FileInfo;
import com.ly.etag.ETagAccess;
import com.ly.rhdfs.file.config.FileInfoManager;

public class ETagAccessDFS implements ETagAccess {

    private FileInfoManager fileInfoManager;

    private String etagFileSuffix;

    public void setFileInfoManager(FileInfoManager fileInfoManager){
        this.fileInfoManager=fileInfoManager;
    }
    @Override
    public String getEtagFileSuffix() {
        return etagFileSuffix;
    }

    @Override
    public void setEtagFileSuffix(String etagFileSuffix) {
        this.etagFileSuffix = etagFileSuffix;
    }

    @Override
    public String readEtag(String filePath, int chunk) {
        FileInfo fileInfo=fileInfoManager.findFileInfo(filePath);
        if (fileInfo==null)
            return null;
        if (chunk>=0 && fileInfo.getFileChunkList().size()>chunk){
            if (fileInfo.getFileChunkList().get(chunk)==null)
                return null;
            else
                return fileInfo.getFileChunkList().get(chunk).getChunkEtag();
        }else if (chunk<0)
            return fileInfo.getEtag();
        return fileInfo.getFileChunkList().get(chunk).getChunkEtag();
    }

    @Override
    public void saveEtag(String filePath, String etag, int chunk) {
        FileInfo fileInfo=fileInfoManager.findFileInfo(filePath);
        if (fileInfo==null)
            return;
        if (chunk>=0 && (fileInfo.getFileChunkList().size()<=chunk || fileInfo.getFileChunkList().get(chunk)==null))
            return;
        if(chunk>=0){
            fileInfo.getFileChunkList().get(chunk).setChunkEtag(etag);
        }else{
            fileInfo.setEtag(etag);
        }
        fileInfoManager.submitFileInfo(fileInfo);
    }
}
