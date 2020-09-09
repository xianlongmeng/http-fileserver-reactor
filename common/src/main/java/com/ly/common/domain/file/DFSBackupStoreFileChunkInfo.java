package com.ly.common.domain.file;

import com.ly.common.domain.DFSPartChunk;

public class DFSBackupStoreFileChunkInfo {
    private int times=0;
    private long createTime;
    private long expire=0;
    private int maxTimes=30;
    private DFSPartChunk dfsPartChunk;

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

    public DFSPartChunk getDfsPartChunk() {
        return dfsPartChunk;
    }

    public void setDfsPartChunk(DFSPartChunk dfsPartChunk) {
        this.dfsPartChunk = dfsPartChunk;
    }
}
