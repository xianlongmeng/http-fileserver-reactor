package com.ly.rhdfs.communicate.command;

import com.ly.common.domain.file.FileDelete;
import com.ly.common.domain.file.FileInfo;

public class DFSCommandFileDelete extends DFSCommand{
    private FileDelete fileDelete;

    public DFSCommandFileDelete(){
        commandType=DFSCommand.CT_FILE_DELETE;
    }

    public FileDelete getFileDelete() {
        return fileDelete;
    }

    public void setFileDelete(FileDelete fileDelete) {
        this.fileDelete = fileDelete;
    }
}
