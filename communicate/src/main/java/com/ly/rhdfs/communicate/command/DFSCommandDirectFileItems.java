package com.ly.rhdfs.communicate.command;

import com.ly.common.domain.file.DirectInfo;

public class DFSCommandDirectFileItems extends DFSCommand{
    private DirectInfo directInfo;

    public DFSCommandDirectFileItems(){
        commandType=DFSCommand.CT_DIRECT_FILE_ITEM;
    }
    public DirectInfo getDirectInfo() {
        return directInfo;
    }

    public void setDirectInfo(DirectInfo directInfo) {
        this.directInfo = directInfo;
    }
}
