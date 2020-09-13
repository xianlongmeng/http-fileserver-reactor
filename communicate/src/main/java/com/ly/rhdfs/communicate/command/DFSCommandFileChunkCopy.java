package com.ly.rhdfs.communicate.command;

import com.ly.common.domain.file.FileChunkCopy;

public class DFSCommandFileChunkCopy extends DFSCommand {

    private FileChunkCopy fileChunkCopy;

    public DFSCommandFileChunkCopy() {
        commandType = DFSCommand.CT_FILE_CHUNK;
    }

    public FileChunkCopy getFileChunkCopy() {
        return fileChunkCopy;
    }

    public void setFileChunkCopy(FileChunkCopy fileChunkCopy) {
        this.fileChunkCopy = fileChunkCopy;
    }
}
