package com.ly.rhdfs.communicate.command;

import com.ly.common.domain.file.FileTransferState;

public class DFSCommandFileTransferState extends DFSCommand {

    private FileTransferState fileTransferState;

    public DFSCommandFileTransferState() {
        commandType = DFSCommand.CT_FILE_TRANSFER_STATE;
        fixLength = 78;
    }

    public FileTransferState getFileTransferState() {
        return fileTransferState;
    }

    public void setFileTransferState(FileTransferState fileTransferState) {
        this.fileTransferState = fileTransferState;
    }
}
