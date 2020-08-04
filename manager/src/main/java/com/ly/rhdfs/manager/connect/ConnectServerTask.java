package com.ly.rhdfs.manager.connect;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.ly.common.domain.server.ServerInfoConfiguration;
import com.ly.common.domain.server.ServerState;
import com.ly.rhdfs.manager.server.ServerManager;

public class ConnectServerTask implements Runnable {

    private int longWaitTime=300;
    private int shortWaitTime=30;
    private ServerManager serverManager;

    public ConnectServerTask(ServerManager serverManager) {
        this.serverManager = serverManager;
    }

    @Override
    public void run() {
        // 循环处理MasterServer连接
        boolean flag = false;
        if (serverManager.getMasterServerConfig() != null) {
            for (Map.Entry<Long, ServerInfoConfiguration> masterConfigEntry : serverManager.getMasterServerConfig()
                    .getMasterServerMap().entrySet()) {
                if (masterConfigEntry.getKey() == null || masterConfigEntry.getValue() == null)
                    continue;
                ServerState serverState = serverManager.findServerState(masterConfigEntry.getKey());
                if (serverState == null || serverState.isOnline())
                    continue;
                serverManager.connectServer(serverState);
                flag = true;
            }
        }
        // 启动下次任务，有未连接：30秒，都已连接：300秒
        if (flag) {
            serverManager.getScheduledThreadPoolExecutor().schedule(this, longWaitTime, TimeUnit.SECONDS);
        } else {
            serverManager.getScheduledThreadPoolExecutor().schedule(this, shortWaitTime, TimeUnit.SECONDS);
        }
    }
}
