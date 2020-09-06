package com.ly.rhdfs.communicate.socket.codec;

import com.ly.common.util.SpringContextUtil;
import com.ly.rhdfs.communicate.socket.parse.DFSCommandParse;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

public class DFSCommandDecoder extends ByteToMessageDecoder {
    private final DFSCommandParse dfsCommandParse;
    {
        dfsCommandParse= SpringContextUtil.getBean(DFSCommandParse.class);
    }
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        Object decoded = decode(ctx, in);
        if (decoded != null) {
            out.add(decoded);
        }
    }

    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) {
        // 查找包头
        return dfsCommandParse.parse(in);
    }
}
