package com.ly.rhdfs.communicate;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.springframework.core.io.buffer.DataBuffer;

import com.ly.common.domain.file.FileTransferInfo;
import com.ly.common.domain.server.ServerState;
import com.ly.rhdfs.communicate.command.DFSCommand;
import com.ly.rhdfs.communicate.handler.EventHandler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import reactor.core.publisher.Flux;
import reactor.netty.Connection;

public interface DFSCommunicate {

    boolean sendCommand(Connection connection, DFSCommand command);

    boolean sendCommandObject(Connection connection, Object commandObj);

    ChannelFuture sendCommandAsync(Connection connection, DFSCommand command);

    ChannelFuture sendCommandObjectAsync(Connection connection, Object commandObj);

    Connection connectServer(ServerState serverState, EventHandler eventHandler);

    Channel serverBind(int port, EventHandler eventHandler);

    CompletableFuture<Integer> sendCommandAsyncReply(Connection connection, DFSCommand command, long timeout, TimeUnit timeUnit);

    CompletableFuture<Integer> sendDataAsyncReply(Connection connection, Object/* command */ msg,long timeout,TimeUnit timeUnit);

    CompletableFuture<Integer> sendFileChunkFinishAsyncReply(Connection connection, Object/* command */ msg,long timeout,TimeUnit timeUnit);

    CompletableFuture<Integer> sendFileFinishCommandAsyncReply(Connection connection, Object/* command */ msg,long timeout,TimeUnit timeUnit);
}
