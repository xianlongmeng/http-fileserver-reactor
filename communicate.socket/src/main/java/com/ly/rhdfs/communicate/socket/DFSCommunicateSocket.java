package com.ly.rhdfs.communicate.socket;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.stereotype.Component;

import com.ly.common.domain.file.FileTransferInfo;
import com.ly.common.domain.server.ServerState;
import com.ly.rhdfs.communicate.DFSCommunicate;
import com.ly.rhdfs.communicate.command.DFSCommand;
import com.ly.rhdfs.communicate.handler.EventHandler;
import com.ly.rhdfs.communicate.socket.codec.DFSCommandDecoder;
import com.ly.rhdfs.communicate.socket.handler.DFSCommandHandler;
import com.ly.rhdfs.communicate.socket.handler.HeartBeatHandler;
import com.ly.rhdfs.communicate.socket.parse.DFSCommandParse;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.handler.timeout.IdleStateHandler;
import reactor.core.publisher.Flux;
import reactor.netty.Connection;
import reactor.netty.DisposableServer;
import reactor.netty.tcp.TcpClient;
import reactor.netty.tcp.TcpServer;

@Component
public class DFSCommunicateSocket implements DFSCommunicate {

    private static final int readerIdle = 60;
    private static final int writerIdle = 100;
    private DisposableServer localServer;
    private DFSCommandParse dfsCommandParse;
    private final Map<UUID,CompletableFuture<Integer>> uuidCompletableFutureMap=new ConcurrentHashMap<>();

    @Autowired
    private void setDfsCommandParse(DFSCommandParse dfsCommandParse) {
        this.dfsCommandParse = dfsCommandParse;
    }

    @Override
    public boolean sendCommand(Connection connection, DFSCommand command) {
        if (connection == null || connection.isDisposed() || !connection.channel().isActive())
            return false;
        connection.channel().write(dfsCommandParse.packageCommand(command));
        return true;
    }

    @Override
    public boolean sendCommandObject(Connection connection, Object commandObj) {
        if (connection == null || connection.isDisposed() || !connection.channel().isActive())
            return false;
        connection.channel().write(dfsCommandParse.packageCommandObject(commandObj));
        return true;
    }

    @Override
    public boolean sendFileChunkObject(Connection connection, FileTransferInfo fileTransferInfo,
            DataBuffer dataBuffer) {
        if (connection == null || connection.isDisposed() || !connection.channel().isActive())
            return false;
        connection.channel().write(dfsCommandParse.packageCommandFileTransfer(fileTransferInfo, dataBuffer));
        return true;
    }

    @Override
    public ChannelFuture sendCommandAsync(Connection connection, DFSCommand command) {
        if (connection == null || connection.isDisposed() || !connection.channel().isActive())
            return null;
        return connection.channel().write(dfsCommandParse.packageCommand(command));
    }

    @Override
    public ChannelFuture sendCommandObjectAsync(Connection connection, Object commandObj) {
        if (connection == null || connection.isDisposed() || !connection.channel().isActive())
            return null;
        return connection.channel().write(dfsCommandParse.packageCommandObject(commandObj));
    }

    @Override
    public ChannelFuture sendFileChunkObjectAsync(Connection connection, FileTransferInfo fileTransferInfo,
            Flux<DataBuffer> dataBuffers) {
        return null;
    }

    @Override
    public Connection connectServer(ServerState serverState, EventHandler eventHandler) {
        AtomicReference<Connection> curConnection = new AtomicReference<>();
        TcpClient.create().handle((inbound, outbound) -> inbound.receive().then()).doOnConnected(connection -> {
            curConnection.set(connection);
            // 处理连接失败，以及重新连接
            connection.addHandlerFirst(new IdleStateHandler(readerIdle, writerIdle, 0));
            connection.addHandlerLast(new HeartBeatHandler(serverState));
            // decode
            connection.addHandlerLast(new DFSCommandDecoder());
            // heart,connect init
            connection.addHandlerLast(new DFSCommandHandler(eventHandler));
            // command
            // serverConnectionMap.put(serverState, connection);
        }).connectNow();
        return curConnection.get();
    }

    @Override
    public Channel serverBind(int port, EventHandler eventHandler) {
        localServer = TcpServer.create().port(port).handle((inbound, outbound) -> inbound.receive().then())
                .doOnConnection(connection -> {
                    // heart,connect init
                    connection.addHandlerFirst(new IdleStateHandler(readerIdle, writerIdle, 0));
                    connection.addHandlerLast("heart-beat", new HeartBeatHandler());
                    // decode
                    connection.addHandlerLast(new DFSCommandDecoder());
                    // command
                    connection.addHandlerLast(new DFSCommandHandler(connection, eventHandler));
                }).bindNow();
        return localServer.channel();
    }

    @Override
    public CompletableFuture<Integer> sendCommandAsyncReply(Connection connection, DFSCommand command) {
        return null;
    }

    @Override
    public CompletableFuture<Integer> sendDataAsyncReply(Connection connection, Object msg) {
        // 如何分包ChunkedStream
        return null;
    }

    @Override
    public CompletableFuture<Integer> sendFileChunkInfoAsyncReply(Connection connection, Object msg) {
        return null;
    }

    @Override
    public CompletableFuture<Integer> sendFileChunkDataAsyncReply(Connection connection, Object msg) {
        return null;
    }

    @Override
    public CompletableFuture<Integer> sendFileChunkFinishAsyncReply(Connection connection, Object msg) {
        return null;
    }

    @Override
    public CompletableFuture<Integer> sendFileFinishCommandAsyncReply(Connection connection, Object msg) {
        return null;
    }
}
