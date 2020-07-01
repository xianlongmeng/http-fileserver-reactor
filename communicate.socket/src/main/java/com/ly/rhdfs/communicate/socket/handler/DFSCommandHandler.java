package com.ly.rhdfs.communicate.socket.handler;

import com.ly.common.domain.server.ServerState;
import com.ly.common.util.SpringContextUtil;
import com.ly.rhdfs.communicate.command.DFSCommand;
import com.ly.rhdfs.communicate.handler.EventHandler;
import com.ly.rhdfs.manager.server.ServerManager;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;

public class DFSCommandHandler extends ChannelDuplexHandler {
    private final EventHandler eventHandler;
    private final ServerManager serverManager;
    private boolean flagHeart=false;

    public DFSCommandHandler(EventHandler eventHandler){
        this.eventHandler=eventHandler;
        serverManager= SpringContextUtil.getBean(ServerManager.class);
    }
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof DFSCommand && eventHandler!=null){
            DFSCommand dfsCommand=(DFSCommand)msg;
            eventHandler.processCommandHandler(dfsCommand);
            if (!flagHeart) {
                ChannelHandler channelHandler = ctx.channel().pipeline().get("heart-beat");
                if (channelHandler instanceof HeartBeatHandler) {
                    HeartBeatHandler heartBeatHandler = (HeartBeatHandler) channelHandler;
                    if (heartBeatHandler.getServerState() == null) {
                        ServerState serverState = serverManager.findServerState4ServerId(dfsCommand.getServerId());
                        if (serverState != null) {
                            heartBeatHandler.setServerState(serverState);
                            flagHeart = true;
                        }
                    }
                }
            }
        }else {
            super.channelRead(ctx, msg);
        }
    }
}
