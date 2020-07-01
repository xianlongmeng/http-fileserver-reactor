package com.ly.common.domain.file;

import java.util.List;

public class ChunkInfo {
    private List<Integer> chunkServerIdList;
    private int index;
    private long position;
    private int length;
    private String chunkEtag;

    // 每个分块对应的服务器列表
    public List<Integer> getChunkServerIdList() {
        return chunkServerIdList;
    }

    public void setChunkServerIdList(List<Integer> chunkServerIdList) {
        this.chunkServerIdList = chunkServerIdList;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public long getPosition() {
        return position;
    }

    public void setPosition(long position) {
        this.position = position;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public String getChunkEtag() {
        return chunkEtag;
    }

    public void setChunkEtag(String chunkEtag) {
        this.chunkEtag = chunkEtag;
    }
}
