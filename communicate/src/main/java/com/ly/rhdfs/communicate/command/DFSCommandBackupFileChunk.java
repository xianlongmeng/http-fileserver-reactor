package com.ly.rhdfs.communicate.command;

import com.ly.common.domain.file.FileInfo;

public class DFSCommandBackupFileChunk extends DFSCommand{
    private FileInfo fileInfo;

    public DFSCommandBackupFileChunk(){
        commandType=DFSCommand.CT_FILE_CHUNK_BACKUP;
    }
    public FileInfo getFileInfo() {
        return fileInfo;
    }

    public void setFileInfo(FileInfo fileInfo) {
        this.fileInfo = fileInfo;
    }
}
