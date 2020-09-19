package com.ly.rhdfs.communicate.command;

import com.ly.common.domain.file.FileTransferInfo;

public class DFSCommandFileTransfer extends DFSCommand {
    private FileTransferInfo fileTransferInfo;

    public DFSCommandFileTransfer() {
        commandType = DFSCommand.CT_FILE_TRANSFER;
        fixLength = 74;
    }

    public FileTransferInfo getFileTransferInfo() {
        return fileTransferInfo;
    }

    public void setFileTransferInfo(FileTransferInfo fileTransferInfo) {
        this.fileTransferInfo = fileTransferInfo;
    }
}
