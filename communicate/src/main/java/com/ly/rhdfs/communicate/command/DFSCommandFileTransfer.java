package com.ly.rhdfs.communicate.command;

import com.ly.common.domain.file.FileTransferInfo;
import io.netty.buffer.ByteBuf;

public class DFSCommandFileTransfer extends DFSCommand{
    private FileTransferInfo fileTransferInfo;

    public DFSCommandFileTransfer(){
        commandType=DFSCommand.CT_FILE_TRANSFER;
        fixLength=78;
    }

    public FileTransferInfo getFileTransferInfo() {
        return fileTransferInfo;
    }

    public void setFileTransferInfo(FileTransferInfo fileTransferInfo) {
        this.fileTransferInfo = fileTransferInfo;
    }
}
