package com.ly.common.domain.file;

import java.util.List;

public class FileInfo {

    private String path;
    private String fileName;
    private long createTime;
    private long lastModifyTime;
    private long size;
    private String etag;
    private boolean chunk;
    // 分块大小，<=0表示不分块
    private int chunkSize;
    // 分块数量
    private int chunkCount;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    // 分块信息，可以是整个FileInfo，可以是一个分块的分片
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

    public int getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(int chunkSize) {
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
