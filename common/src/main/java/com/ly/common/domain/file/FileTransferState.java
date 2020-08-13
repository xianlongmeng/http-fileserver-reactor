package com.ly.common.domain.file;

public class FileTransferState extends AbstractFileTransfer {

    private int state;

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }
}
