package com.ly.rhdfs.communicate.socket.handler;

import com.ly.common.domain.server.ServerState;
import com.ly.common.util.SpringContextUtil;
import com.ly.rhdfs.communicate.command.DFSCommand;
import com.ly.rhdfs.communicate.handler.EventHandler;
import com.ly.rhdfs.manager.connect.ConnectManager;
import com.ly.rhdfs.manager.server.ServerManager;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import reactor.netty.Connection;

public class DFSCommandHandler extends ChannelDuplexHandler {

    private final EventHandler eventHandler;
    private final ServerManager serverManager;
    private final ConnectManager connectManager;
    private final Connection connection;
    private boolean flagHeart = false;

    public DFSCommandHandler(EventHandler eventHandler) {
        this.eventHandler = eventHandler;
        serverManager = SpringContextUtil.getBean(ServerManager.class);
        connectManager = SpringContextUtil.getBean(ConnectManager.class);
        this.connection = null;
        this.flagHeart = false;
    }

    public DFSCommandHandler(Connection connection, EventHandler eventHandler) {
        this.eventHandler = eventHandler;
        serverManager = SpringContextUtil.getBean(ServerManager.class);
        connectManager = SpringContextUtil.getBean(ConnectManager.class);
        this.connection = connection;
        this.flagHeart = true;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof DFSCommand && eventHandler != null) {
            DFSCommand dfsCommand = (DFSCommand) msg;
            if (!flagHeart) {
                ChannelHandler channelHandler = ctx.channel().pipeline().get("heart-beat");
                if (channelHandler instanceof HeartBeatHandler) {
                    HeartBeatHandler heartBeatHandler = (HeartBeatHandler) channelHandler;
                    if (heartBeatHandler.getServerState() == null) {
                        ServerState serverState = serverManager.findServerState(dfsCommand.getServerId());
                        if (serverState != null) {
                            heartBeatHandler.setServerState(serverState);
                            if (!connectManager.putServerConnection(serverState, connection)) {
                                connection.dispose();
                            }
                            flagHeart = true;
                        }
                    }
                }
            }
            eventHandler.processCommandHandler(dfsCommand);
        } else {
            super.channelRead(ctx, msg);
        }
    }
}
