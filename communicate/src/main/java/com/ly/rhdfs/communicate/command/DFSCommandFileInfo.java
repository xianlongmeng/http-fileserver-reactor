package com.ly.rhdfs.communicate.command;

import com.ly.common.domain.file.FileInfo;

public class DFSCommandFileInfo extends DFSCommand{
    private FileInfo fileInfo;

    public DFSCommandFileInfo(){
        commandType=DFSCommand.CT_FILE_INFO;
    }
    public FileInfo getFileInfo() {
        return fileInfo;
    }

    public void setFileInfo(FileInfo fileInfo) {
        this.fileInfo = fileInfo;
    }
}
