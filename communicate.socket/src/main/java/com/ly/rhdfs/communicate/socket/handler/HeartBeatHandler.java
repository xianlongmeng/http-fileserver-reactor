package com.ly.rhdfs.communicate.socket.handler;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ly.common.domain.server.ServerState;
import com.ly.common.util.SpringContextUtil;
import com.ly.rhdfs.manager.server.ServerManager;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

public class HeartBeatHandler extends ChannelInboundHandlerAdapter {

    private final ServerManager serverManager;
    private Logger logger = LoggerFactory.getLogger(this.getClass());
    private boolean server;
    private ServerState serverState;

    public HeartBeatHandler() {
        this(true, null);
    }

    public HeartBeatHandler(ServerState serverState) {
        this(false, serverState);
    }

    public HeartBeatHandler(boolean server, ServerState serverState) {
        this.server = server;
        this.serverState = serverState;
        serverManager = SpringContextUtil.getBean(ServerManager.class);
    }

    private synchronized void reconnect(ChannelHandlerContext ctx) {
        ctx.close();
        if (serverState != null) {
            serverState.setOnline(false);
            if (serverManager.getLocalServerState().getType() != ServerState.SIT_MASTER
                    && serverState.getType() == ServerState.SIT_MASTER
                    && serverManager.getMasterServerId() == serverState.getServerId()) {
                serverManager.getLocalServerState()
                        .setState(serverManager.getLocalServerState().getState() | ServerState.SIS_MASTER_LOST_CONTACT);
            }
        }
        if (!server) {
            // 重连
            logger.error("reconnect:{}", ctx.channel().remoteAddress());
            // trigger reconnect
            serverManager.reconnectServer(serverState);
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state().equals(IdleState.READER_IDLE) || event.state().equals(IdleState.ALL_IDLE)) {
                // 读超时
                logger.error("read idle-{}", ctx.channel().remoteAddress());
                reconnect(ctx);
            }
            if ((event.state().equals(IdleState.WRITER_IDLE) || event.state().equals(IdleState.ALL_IDLE))) {
                // 发送心跳，保持长连接
                serverManager.sendHeart(serverState);
            }
        }
        super.userEventTriggered(ctx, evt);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        // 需要关闭连接
        logger.error("inactive-{}", ctx.channel().remoteAddress());
        reconnect(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        reconnect(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        serverState.setOnline(true);
        serverState.setLastTime(Instant.now().toEpochMilli());
        super.channelRead(ctx, msg);
    }

    public boolean isServer() {
        return server;
    }

    public ServerState getServerState() {
        return serverState;
    }

    public void setServerState(ServerState serverState) {
        this.serverState = serverState;
    }
}
