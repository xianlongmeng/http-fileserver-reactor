package com.ly.rhdfs.communicate.socket;

import reactor.netty.Connection;
import reactor.netty.DisposableServer;
import reactor.netty.tcp.TcpClient;

public class ConnectionInfo {
    private boolean server;
    private Connection connection;
}
