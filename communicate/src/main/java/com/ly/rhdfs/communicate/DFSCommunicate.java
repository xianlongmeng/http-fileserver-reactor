package com.ly.rhdfs.communicate;

import java.util.function.Consumer;

import com.ly.common.domain.server.ServerState;
import com.ly.rhdfs.communicate.command.DFSCommand;
import com.ly.rhdfs.communicate.handler.EventHandler;

import io.netty.channel.Channel;
import reactor.netty.Connection;

public interface DFSCommunicate {

    boolean sendCommand(ServerState serverState, DFSCommand command);

    boolean sendCommandObject(ServerState serverState, Object commandObj);

    Connection connectServer(ServerState serverState, EventHandler eventHandler);

    Channel serverBind(int port, EventHandler eventHandler);
}
