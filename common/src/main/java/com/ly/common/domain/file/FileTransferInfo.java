package com.ly.common.domain.file;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.NettyDataBufferFactory;

import io.netty.buffer.ByteBuf;

public class FileTransferInfo extends AbstractFileTransfer {

    private int size;
    private ByteBuf byteBuf;

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public ByteBuf getByteBuf() {
        return byteBuf;
    }

    public void setByteBuf(ByteBuf byteBuf) {
        this.byteBuf = byteBuf;
    }

    public void setByteBuf(DataBuffer dataBuffer) {
        if (dataBuffer == null)
            this.byteBuf = null;
        else
            this.byteBuf = NettyDataBufferFactory.toByteBuf(dataBuffer);
    }
}
