package com.ly.common.domain.file;

import java.time.Instant;

public class BackupMasterFileInfo {
    public static final int TYPE_ADD=0;
    public static final int TYPE_DELETE=1;
    private int times = 0;
    private long createTime;
    private long expire = 0;
    private int maxTimes = 0;
    private int type;
    private FileInfo fileInfo;
    public BackupMasterFileInfo(int type,FileInfo fileInfo){
        this.type=type;
        this.fileInfo=fileInfo;
        createTime= Instant.now().toEpochMilli();
    }

    public int getTimes() {
        return times;
    }

    public void setTimes(int times) {
        this.times = times;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public long getExpire() {
        return expire;
    }

    public void setExpire(long expire) {
        this.expire = expire;
    }

    public int getMaxTimes() {
        return maxTimes;
    }

    public void setMaxTimes(int maxTimes) {
        this.maxTimes = maxTimes;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public FileInfo getFileInfo() {
        return fileInfo;
    }

    public void setFileInfo(FileInfo fileInfo) {
        this.fileInfo = fileInfo;
    }
}
