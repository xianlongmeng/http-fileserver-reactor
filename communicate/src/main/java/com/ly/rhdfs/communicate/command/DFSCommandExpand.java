package com.ly.rhdfs.communicate.command;

public class DFSCommandExpand extends DFSCommand{
    private byte[] bytes;

    public DFSCommandExpand(){
        commandType=DFSCommand.CT_REQUEST_EXPAND;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }
}
