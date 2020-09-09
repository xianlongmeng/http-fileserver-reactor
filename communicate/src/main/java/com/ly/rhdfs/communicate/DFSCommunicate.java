package com.ly.rhdfs.communicate;

import com.ly.common.domain.DFSPartChunk;
import com.ly.common.domain.server.ServerState;
import com.ly.rhdfs.communicate.command.DFSCommand;
import com.ly.rhdfs.communicate.command.DFSCommandFileTransfer;
import com.ly.rhdfs.communicate.command.DFSCommandReply;
import com.ly.rhdfs.communicate.handler.EventHandler;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import reactor.core.publisher.Flux;
import reactor.netty.Connection;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public interface DFSCommunicate {

    boolean sendCommand(Connection connection, DFSCommand command);

    boolean sendCommandObject(Connection connection, Object commandObj, int commandType);

    boolean sendCommandSync(Connection connection, DFSCommand command);

    boolean sendCommandObjectSync(Connection connection, Object commandObj, int commandType);

    boolean sendFileInfoCommandObjectSync(Connection connection, byte[] fileInfo);

    ChannelFuture sendCommandAsync(Connection connection, DFSCommand command);

    ChannelFuture sendCommandObjectAsync(Connection connection, Object commandObj, int commandType);

    Connection connectServer(ServerState serverState, EventHandler eventHandler);

    Channel serverBind(int port, EventHandler eventHandler);

    CompletableFuture<Integer> sendCommandAsyncReply(Connection connection, DFSCommand command, long timeout, TimeUnit timeUnit);

    CompletableFuture<Integer> sendCommandDataAsyncReply(Connection connection, Flux<ByteBuf> byteBufFlux,
                                                         DFSCommandFileTransfer dfsCommandFileTransfer, long timeout, TimeUnit timeUnit);

    CompletableFuture<Integer> sendDataAsyncReply(Connection connection, Object/* command */ msg, int commandType, long timeout, TimeUnit timeUnit);

    CompletableFuture<Integer> sendFileChunkFinishAsyncReply(Connection connection, Object/* command */ msg, long timeout, TimeUnit timeUnit);

    CompletableFuture<Integer> sendFileFinishCommandAsyncReply(Connection connection, Object/* command */ msg, long timeout, TimeUnit timeUnit);

    boolean sendCommandReply(Connection connection,DFSCommand dfsCommand,byte replyResult);
    void receiveReply(DFSCommandReply dfsCommandReply);
}
