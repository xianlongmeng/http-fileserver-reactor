package com.ly.rhdfs.manager.connect;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import com.ly.rhdfs.communicate.command.DFSCommandReply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ly.common.domain.server.ServerState;
import com.ly.rhdfs.communicate.DFSCommunicate;
import com.ly.rhdfs.communicate.command.DFSCommand;
import com.ly.rhdfs.communicate.command.DFSCommandFileTransfer;
import com.ly.rhdfs.communicate.handler.EventHandler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import reactor.core.publisher.Flux;
import reactor.netty.Connection;

@Component
public class ConnectManager {

    private final Map<ServerState, Connection> serverConnectionMap = new ConcurrentHashMap<>();
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private DFSCommunicate dfsCommunicate;
    private Channel socketListenChannel;

    @Autowired
    public void setDfsCommunicate(DFSCommunicate dfsCommunicate) {
        this.dfsCommunicate = dfsCommunicate;
    }

    public Connection findConnection(ServerState serverState) {
        if (serverState == null)
            return null;
        return serverConnectionMap.get(serverState);
    }

    public boolean putServerConnection(ServerState serverState, Connection connection) {
        return putServerConnection(serverState, connection, null);
    }

    public synchronized boolean putServerConnection(ServerState serverState, Connection connection,
            Connection oldConnection) {
        if (serverState == null || connection == null)
            return false;
        Connection curConnection = serverConnectionMap.get(serverState);
        if (curConnection != oldConnection) {
            connection.dispose();
            logger.info("connection is cancel,repetition!address:{} port:{} serverId:{}", serverState.getAddress(),
                    serverState.getPort(), serverState.getServerId());
            return false;
        }
        serverConnectionMap.put(serverState, connection);
        if (!serverState.isOnline()) {
            serverState.setOnline(true);
            return true;
        } else
            return false;
    }

    public void startSocketListen(int port, EventHandler eventHandler) {
        socketListenChannel = dfsCommunicate.serverBind(port, eventHandler);
    }

    public void startConnectServer(ServerState serverState, EventHandler eventHandler) {
        if (serverState == null || serverState.isOnline())
            return;
        Connection connection = dfsCommunicate.connectServer(serverState, eventHandler);
        if (connection != null && !putServerConnection(serverState, connection)) {
            connection.dispose();
        }
    }

    public void closeServer(ServerState serverState) {
        if (serverState == null)
            return;
        Connection connection = serverConnectionMap.get(serverState);
        connection.disposeNow();
    }

    public boolean sendCommunicationObject(ServerState serverState, Object commandObj, int commandType) {
        return dfsCommunicate.sendCommandObject(findConnection(serverState), commandObj, commandType);
    }

    public boolean sendCommunication(ServerState serverState, DFSCommand dfsCommand) {
        return dfsCommunicate.sendCommand(findConnection(serverState), dfsCommand);
    }

    public boolean sendCommunicationObjectSync(ServerState serverState, Object commandObj, int commandType) {
        return dfsCommunicate.sendCommandObjectSync(findConnection(serverState), commandObj, commandType);
    }

    public boolean sendCommunicationSync(ServerState serverState, DFSCommand dfsCommand) {
        return dfsCommunicate.sendCommandSync(findConnection(serverState), dfsCommand);
    }

    public boolean sendFileInfoCommandSync(ServerState serverState, byte[] fileInfo) {
        return dfsCommunicate.sendFileInfoCommandObjectSync(findConnection(serverState), fileInfo);
    }

    public ChannelFuture sendCommunicationObjectAsync(ServerState serverState, Object commandObj, int commandType) {
        return dfsCommunicate.sendCommandObjectAsync(findConnection(serverState), commandObj, commandType);
    }

    public ChannelFuture sendCommunicationAsync(ServerState serverState, DFSCommand dfsCommand) {
        return dfsCommunicate.sendCommandAsync(findConnection(serverState), dfsCommand);
    }

    public CompletableFuture<Integer> sendCommandAsyncReply(ServerState serverState, DFSCommand dfsCommand, long timeout, TimeUnit timeUnit) {
        return dfsCommunicate.sendCommandAsyncReply(findConnection(serverState), dfsCommand, timeout, timeUnit);
    }
    public CompletableFuture<Integer> sendDataAsyncReply(ServerState serverState, Object/* command */ msg, int commandType, long timeout, TimeUnit timeUnit){
        return dfsCommunicate.sendDataAsyncReply(findConnection(serverState),msg,commandType,timeout,timeUnit);
    }
    public CompletableFuture<Integer> sendCommandDataAsyncReply(ServerState serverState, Flux<ByteBuf> byteBufFlux,
            DFSCommandFileTransfer dfsCommandFileTransfer, long timeout, TimeUnit timeUnit) {
        return dfsCommunicate.sendCommandDataAsyncReply(findConnection(serverState), byteBufFlux,
                dfsCommandFileTransfer, timeout, timeUnit);
    }
    public boolean sendCommandReply(ServerState serverState,DFSCommand dfsCommand,byte replyResult,int errorCode){
        if (dfsCommand==null)
            return false;
        return dfsCommunicate.sendCommandReply(findConnection(serverState),dfsCommand,replyResult,errorCode);
    }
    public void receiveReply(DFSCommandReply dfsCommandReply) {
        dfsCommunicate.receiveReply(dfsCommandReply);
    }
}
