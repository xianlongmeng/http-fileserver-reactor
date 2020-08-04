package com.ly.common.domain.file;

import java.util.ArrayList;
import java.util.List;

public class ChunkInfo {
    public static final int FILE_CHUNK_STATE_OK=1;
    public static final int FILE_CHUNK_STATE_NONE=0;
    private final List<Long> chunkServerIdList=new ArrayList<>();
    private int index;
    private int state=FILE_CHUNK_STATE_NONE;
    private String chunkEtag;

    // 每个分块对应的服务器列表
    public List<Long> getChunkServerIdList() {
        return chunkServerIdList;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public String getChunkEtag() {
        return chunkEtag;
    }

    public void setChunkEtag(String chunkEtag) {
        this.chunkEtag = chunkEtag;
    }
}
