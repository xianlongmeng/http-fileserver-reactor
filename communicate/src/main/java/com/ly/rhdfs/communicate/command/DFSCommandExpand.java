package com.ly.rhdfs.communicate.command;

import io.netty.buffer.ByteBuf;

public class DFSCommandExpand extends DFSCommand{
    private ByteBuf byteBuf;

    public DFSCommandExpand(){
        commandType=DFSCommand.CT_REQUEST_EXPAND;
    }
    public ByteBuf getByteBuf() {
        return byteBuf;
    }

    public void setByteBuf(ByteBuf byteBuf) {
        this.byteBuf = byteBuf;
    }
}
