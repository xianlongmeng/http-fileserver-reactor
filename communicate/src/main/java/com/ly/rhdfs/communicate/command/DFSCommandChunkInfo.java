package com.ly.rhdfs.communicate.command;

import com.ly.common.domain.file.ChunkInfo;

public class DFSCommandChunkInfo extends DFSCommand{
    private ChunkInfo chunkInfo;
    public DFSCommandChunkInfo(){
        commandType=DFSCommand.CT_FILE_CHUNK;
    }

    public ChunkInfo getChunkInfo() {
        return chunkInfo;
    }

    public void setChunkInfo(ChunkInfo chunkInfo) {
        this.chunkInfo = chunkInfo;
    }
}
