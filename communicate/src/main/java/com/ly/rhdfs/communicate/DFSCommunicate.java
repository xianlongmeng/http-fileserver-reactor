package com.ly.rhdfs.communicate;

import com.ly.common.domain.server.ServerState;
import com.ly.rhdfs.communicate.command.DFSCommand;
import com.ly.rhdfs.communicate.handler.EventHandler;
import reactor.core.publisher.Mono;
import reactor.netty.Connection;
import reactor.netty.DisposableChannel;

public interface DFSCommunicate {
    Mono<Boolean> sendCommand(ServerState serverState, DFSCommand command);
    Connection connectServer(ServerState serverState, EventHandler eventHandler);
    Mono<DisposableChannel> serverBind(int port, EventHandler eventHandler);
}
