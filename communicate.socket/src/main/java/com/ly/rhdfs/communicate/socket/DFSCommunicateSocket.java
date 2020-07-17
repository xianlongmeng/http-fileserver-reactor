package com.ly.rhdfs.communicate.socket;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ly.common.domain.server.ServerState;
import com.ly.rhdfs.communicate.DFSCommunicate;
import com.ly.rhdfs.communicate.command.DFSCommand;
import com.ly.rhdfs.communicate.handler.EventHandler;
import com.ly.rhdfs.communicate.socket.codec.DFSCommandDecoder;
import com.ly.rhdfs.communicate.socket.handler.DFSCommandHandler;
import com.ly.rhdfs.communicate.socket.handler.HeartBeatHandler;
import com.ly.rhdfs.communicate.socket.parse.DFSCommandParse;

import io.netty.channel.Channel;
import io.netty.handler.timeout.IdleStateHandler;
import reactor.netty.Connection;
import reactor.netty.DisposableServer;
import reactor.netty.tcp.TcpClient;
import reactor.netty.tcp.TcpServer;

@Component
public class DFSCommunicateSocket implements DFSCommunicate {

    private static final int readerIdle = 60;
    private static final int writerIdle = 100;
    private final Map<ServerState, Connection> serverConnectionMap = new ConcurrentHashMap<>();
    private DisposableServer localServer;
    private DFSCommandParse dfsCommandParse;
    @Autowired
    private void setDfsCommandParse(DFSCommandParse dfsCommandParse){
        this.dfsCommandParse=dfsCommandParse;
    }

    private Connection findConnection(ServerState serverState){
        if (serverState==null)
            return null;
        return serverConnectionMap.get(serverState);
    }
    @Override
    public boolean sendCommand(ServerState serverState, DFSCommand command) {
        Connection connection=findConnection(serverState);
        if (connection==null || connection.isDisposed() || !connection.channel().isActive())
            return false;
        connection.channel().write(dfsCommandParse.packageCommand(command));
        return true;
    }

    @Override
    public boolean sendCommandObject(ServerState serverState, Object commandObj) {
        Connection connection=findConnection(serverState);
        if (connection==null || connection.isDisposed() || !connection.channel().isActive())
            return false;
        connection.channel().write(dfsCommandParse.packageCommandObject(commandObj));
        return true;
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
                    connection.addHandlerLast(new DFSCommandHandler(connection,eventHandler));
                }).bindNow();
        return localServer.channel();
    }
}
