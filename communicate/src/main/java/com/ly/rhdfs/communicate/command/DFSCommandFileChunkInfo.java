package com.ly.rhdfs.communicate.command;

import com.ly.common.domain.file.FileChunkInfo;

public class DFSCommandFileChunkInfo extends DFSCommand {
    private FileChunkInfo fileChunkInfo;

    public DFSCommandFileChunkInfo() {
        commandType = DFSCommand.CT_FILE_CHUNK;
    }

    public FileChunkInfo getFileChunkInfo() {
        return fileChunkInfo;
    }

    public void setFileChunkInfo(FileChunkInfo fileChunkInfo) {
        this.fileChunkInfo = fileChunkInfo;
    }
}
