package com.ly.rhdfs.communicate;

import com.ly.common.domain.server.ServerState;
import com.ly.rhdfs.communicate.command.DFSCommand;
import com.ly.rhdfs.communicate.handler.EventHandler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import reactor.core.publisher.FluxSink;
import reactor.netty.Connection;
import reactor.util.annotation.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Consumer;

public interface DFSCommunicate {

    boolean sendCommand(ServerState serverState, DFSCommand command);

    boolean sendCommandObject(ServerState serverState, Object commandObj);

    Connection connectServer(ServerState serverState, EventHandler eventHandler);

    Channel serverBind(int port, EventHandler eventHandler);

    CompletableFuture<Integer> sendTaskAsyncReply(Object msg);
}
