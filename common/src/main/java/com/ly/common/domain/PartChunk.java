package com.ly.common.domain;

public class PartChunk {

    // 是否分片
    private boolean chunked;
    // 当前片序号
    private int chunk = 0;
    // 每片的尺寸
    private int chunkSize = 0;
    //分片数量
    private int chunkCount = 0;

    public PartChunk(boolean chunked) {
        this.chunked = chunked;
    }

    public PartChunk(boolean chunked, int chunk, int chunkSize, int chunkCount) {
        this.chunked = chunked;
        this.chunk = chunk;
        this.chunkSize = chunkSize;
        this.chunkCount = chunkCount;
    }
    public boolean isChunked() {
        return chunked;
    }

    public void setChunked(boolean chunked) {
        this.chunked = chunked;
    }

    public int getChunk() {
        return chunk;
    }

    public void setChunk(int chunk) {
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
}
