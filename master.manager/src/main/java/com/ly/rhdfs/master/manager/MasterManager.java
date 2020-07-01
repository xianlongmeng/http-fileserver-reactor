package com.ly.rhdfs.master.manager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ly.common.domain.server.ServerState;
import com.ly.rhdfs.manager.config.ServerConfig;
import com.ly.rhdfs.manager.connect.ConnectManager;
import com.ly.rhdfs.manager.handler.CommandEventHandler;
import com.ly.rhdfs.manager.server.ServerManager;

@Component
public class MasterManager {

    private ServerManager serverManager;
    private ServerConfig serverConfig;
    private ConnectManager connectManager;
    private ServerState currentServerState;

    @Autowired
    public void setServerManager(ServerManager serverManager) {
        this.serverManager = serverManager;
    }

    @Autowired
    public void setServerConfig(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    @Autowired
    public void setConnectManager(ConnectManager connectManager) {
        this.connectManager = connectManager;
    }

    public void initial() {
        // 初始化ServerManager
        serverManager.loadMasterServer(serverConfig.getConfigPath());
        // 初始化监听
        connectManager.startMasterListen(serverConfig.getPort(), new CommandEventHandler());
        // 定时连接线程
        // 定时配置更新
        // 定时统计投票计数，主从状态
        // 定时发送心跳
    }
    public void resetServerState(){

    }
    public boolean verifyMasterQualification(){
        //exist master
        //verify count(self record count,other master record count
        //verify updateTime
        //
        return false;
    }
}
