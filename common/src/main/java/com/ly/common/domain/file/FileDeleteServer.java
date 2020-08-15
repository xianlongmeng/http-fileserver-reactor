package com.ly.common.domain.file;

import java.time.Instant;

public class FileDeleteServer {
    private FileDelete fileDelete;
    private long serverId;
    private long timestamp;

    public FileDeleteServer(FileDelete fileDelete,long serverId){
        this.fileDelete=fileDelete;
        this.serverId=serverId;
        timestamp= Instant.now().toEpochMilli();
    }
    public FileDelete getFileDelete() {
        return fileDelete;
    }

    public void setFileDelete(FileDelete fileDelete) {
        this.fileDelete = fileDelete;
    }

    public long getServerId() {
        return serverId;
    }

    public void setServerId(long serverId) {
        this.serverId = serverId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
