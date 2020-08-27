package com.ly.rhdfs.communicate.socket;

import com.fasterxml.uuid.Generators;
import com.ly.common.domain.ResultInfo;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.netty.Connection;
import reactor.netty.DisposableServer;
import reactor.netty.tcp.TcpClient;
import reactor.netty.tcp.TcpServer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class DFSCommunicateSocket implements DFSCommunicate {

    private static final int readerIdle = 60;
    private static final int writerIdle = 100;
    private final Map<UUID, CompletableFuture<Integer>> uuidCompletableFutureMap = new ConcurrentHashMap<>();
    private DisposableServer localServer;
    private DFSCommandParse dfsCommandParse;

    @Autowired
    private void setDfsCommandParse(DFSCommandParse dfsCommandParse) {
        this.dfsCommandParse = dfsCommandParse;
    }

    public CompletableFuture<Integer> findFuture(UUID uuid) {
        if (uuid == null)
            return null;
        return uuidCompletableFutureMap.get(uuid);
    }

    public CompletableFuture<Integer> findFuture(long mostSigBits, long leastSigBits) {
        return uuidCompletableFutureMap.get(new UUID(mostSigBits, leastSigBits));
    }

    @Override
    public boolean sendCommand(Connection connection, DFSCommand command) {
        if (connection == null || connection.isDisposed() || !connection.channel().isActive())
            return false;
        connection.channel().write(dfsCommandParse.packageCommand(command));
        return true;
    }

    @Override
    public boolean sendCommandObject(Connection connection, Object commandObj, int commandType) {
        return sendCommand(connection, dfsCommandParse.convertCommandObject(commandObj, commandType));
    }

    @Override
    public boolean sendCommandSync(Connection connection, DFSCommand command) {
        if (connection == null || connection.isDisposed() || !connection.channel().isActive())
            return false;
        try {
            return connection.channel().write(dfsCommandParse.packageCommand(command)).sync().isSuccess();
        } catch (InterruptedException e) {
            return false;
        }
    }

    @Override
    public boolean sendCommandObjectSync(Connection connection, Object commandObj, int commandType) {
        return sendCommandSync(connection, dfsCommandParse.convertCommandObject(commandObj, commandType));
    }

    @Override
    public boolean sendFileInfoCommandObjectSync(Connection connection, byte[] fileInfo) {
        if (connection == null || connection.isDisposed() || !connection.channel().isActive())
            return false;
        try {
            return connection.channel().write(dfsCommandParse.packageCommandFileInfo(fileInfo)).sync().isSuccess();
        } catch (InterruptedException e) {
            return false;
        }
    }

    @Override
    public ChannelFuture sendCommandAsync(Connection connection, DFSCommand command) {
        if (connection == null || connection.isDisposed() || !connection.channel().isActive())
            return null;
        return connection.channel().write(dfsCommandParse.packageCommand(command));
    }

    @Override
    public ChannelFuture sendCommandObjectAsync(Connection connection, Object commandObj, int commandType) {
        return sendCommandAsync(connection, dfsCommandParse.convertCommandObject(commandObj, commandType));
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
    public CompletableFuture<Integer> sendCommandAsyncReply(Connection connection, DFSCommand command, long timeout, TimeUnit timeUnit) {
        CompletableFuture<Integer> completableFuture;
        if (timeout > 0 && timeUnit != null) {
            completableFuture = new CompletableFuture<Integer>().orTimeout(timeout, timeUnit)
                    .exceptionally(throwable -> {
                        uuidCompletableFutureMap.remove(command.getUuid());
                        return ResultInfo.S_FAILED_TIMEOUT;
                    });
        } else {
            completableFuture = new CompletableFuture<>();
        }
        if (command == null) {
            completableFuture.complete(ResultInfo.S_ERROR);
            return completableFuture;
        }
        if (command.getUuid() == null)
            command.setUuid(Generators.timeBasedGenerator().generate());
        uuidCompletableFutureMap.put(command.getUuid(), completableFuture);
        ChannelFuture channelFuture = sendCommandAsync(connection, command);
        channelFuture.addListener(futureListen -> {
            if (!futureListen.isSuccess()) {
                completableFuture.complete(ResultInfo.S_FAILED);
                uuidCompletableFutureMap.remove(command.getUuid());
            }
        });
        return completableFuture;
    }

    @Override
    public CompletableFuture<Integer> sendDataAsyncReply(Connection connection, Object msg, int commandType, long timeout, TimeUnit timeUnit) {
        // 如何分包ChunkedStream
        return sendCommandAsyncReply(connection, dfsCommandParse.convertCommandObject(msg, commandType), timeout, timeUnit);
    }

    @Override
    public CompletableFuture<Integer> sendFileChunkFinishAsyncReply(Connection connection, Object msg, long timeout, TimeUnit timeUnit) {
        return null;
    }

    @Override
    public CompletableFuture<Integer> sendFileFinishCommandAsyncReply(Connection connection, Object msg, long timeout, TimeUnit timeUnit) {
        return null;
    }
}
