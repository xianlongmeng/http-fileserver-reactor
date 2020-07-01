package com.ly.common.domain.file;

import java.util.List;

public class FileInfo {

    private long createTime;
    private long lastModifyTime;
    private long size;
    private String etag;
    private boolean chunk;
    // 分块大小，>=0表示不分块
    private long chunkSize;
    // 分块数量
    private int chunkCount;
    // 分块信息
    private List<ChunkInfo> fileChunkList;

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public long getLastModifyTime() {
        return lastModifyTime;
    }

    public void setLastModifyTime(long lastModifyTime) {
        this.lastModifyTime = lastModifyTime;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getEtag() {
        return etag;
    }

    public void setEtag(String etag) {
        this.etag = etag;
    }

    public boolean isChunk() {
        return chunk;
    }

    public void setChunk(boolean chunk) {
        this.chunk = chunk;
    }

    public long getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(long chunkSize) {
        this.chunkSize = chunkSize;
    }

    public int getChunkCount() {
        return chunkCount;
    }

    public void setChunkCount(int chunkCount) {
        this.chunkCount = chunkCount;
    }

    public List<ChunkInfo> getFileChunkList() {
        return fileChunkList;
    }

    public void setFileChunkList(List<ChunkInfo> fileChunkList) {
        this.fileChunkList = fileChunkList;
    }
}
