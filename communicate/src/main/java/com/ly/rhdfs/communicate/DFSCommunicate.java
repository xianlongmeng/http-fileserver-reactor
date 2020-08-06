package com.ly.rhdfs.communicate;

import java.util.concurrent.CompletableFuture;

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

    boolean sendFileChunkObject(Connection connection, FileTransferInfo fileTransferInfo,
                                DataBuffer dataBuffer);

    ChannelFuture sendCommandAsync(Connection connection, DFSCommand command);

    ChannelFuture sendCommandObjectAsync(Connection connection, Object commandObj);

    ChannelFuture sendFileChunkObjectAsync(Connection connection, FileTransferInfo fileTransferInfo,
                                Flux<DataBuffer> dataBuffers);

    Connection connectServer(ServerState serverState, EventHandler eventHandler);

    Channel serverBind(int port, EventHandler eventHandler);

    CompletableFuture<Integer> sendCommandAsyncReply(Connection connection, DFSCommand command);

    CompletableFuture<Integer> sendDataAsyncReply(Connection connection, Object/* command */ msg);

    CompletableFuture<Integer> sendFileChunkInfoAsyncReply(Connection connection, Object/* command */ msg);

    CompletableFuture<Integer> sendFileChunkDataAsyncReply(Connection connection, Object/* buffer */ msg);

    CompletableFuture<Integer> sendFileChunkFinishAsyncReply(Connection connection, Object/* command */ msg);

    CompletableFuture<Integer> sendFileFinishCommandAsyncReply(Connection connection, Object/* command */ msg);
}
