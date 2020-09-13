package com.ly.common.domain.log;

import com.ly.common.domain.file.FileInfo;

public class UpdateChunkServer {
    private FileInfo fileInfo;
    private int chunk;
    private long oldServerId;
    private long newServerId;
    private long sourceServerId=-1;
    private int sourceIndex=-1;

    public UpdateChunkServer(FileInfo fileInfo,int chunk,long oldServerId,long newServerId){
        this.fileInfo=fileInfo;
        this.chunk=chunk;
        this.oldServerId=oldServerId;
        this.newServerId=newServerId;
    }
    public FileInfo getFileInfo() {
        return fileInfo;
    }

    public void setFileInfo(FileInfo fileInfo) {
        this.fileInfo = fileInfo;
    }

    public int getChunk() {
        return chunk;
    }

    public void setChunk(int chunk) {
        this.chunk = chunk;
    }

    public long getOldServerId() {
        return oldServerId;
    }

    public void setOldServerId(long oldServerId) {
        this.oldServerId = oldServerId;
    }

    public long getNewServerId() {
        return newServerId;
    }

    public void setNewServerId(long newServerId) {
        this.newServerId = newServerId;
    }

    public long getSourceServerId() {
        return sourceServerId;
    }

    public void setSourceServerId(long sourceServerId) {
        this.sourceServerId = sourceServerId;
    }

    public int getSourceIndex() {
        return sourceIndex;
    }

    public void setSourceIndex(int sourceIndex) {
        this.sourceIndex = sourceIndex;
    }
}
